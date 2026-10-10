package com.gymtracker.android.presentation.composables.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gymtracker.android.domain.model.RunPoint
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource

/** Tiles are only a backdrop: the supplied recorded points own the displayed route. */
@Composable
fun RunMap(points: List<RunPoint>, position: RunPoint?, modifier: Modifier = Modifier, live: Boolean = true) {
    val dark = MaterialTheme.colorScheme.background.red < 0.5f
    if (LocalInspectionMode.current) {
        Surface(modifier) { Text("Run map", Modifier.padding(24.dp)) }
        return
    }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var error by remember { mutableStateOf<String?>(null) }
    val holder = remember {
        MapLibre.getInstance(context)
        RunMapHolder(MapView(context), live) { error = it }
    }
    DisposableEffect(holder, owner) {
        holder.view.onCreate(null)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) holder.view.onStart()
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) holder.view.onResume()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> holder.view.onStart()
                Lifecycle.Event.ON_RESUME -> holder.view.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.view.onPause()
                Lifecycle.Event.ON_STOP -> holder.view.onStop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            holder.view.onPause(); holder.view.onStop(); holder.view.onDestroy()
        }
    }
    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(factory = { holder.view.apply {
                getMapAsync { holder.attach(it) }
            } }, modifier = Modifier.fillMaxSize(), update = {
                holder.update(points, position, dark)
            })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { holder.recenter() }, enabled = position != null || points.isNotEmpty()) { Text("Recenter") }
            if (error != null) TextButton(onClick = { error = null; holder.reload() }) { Text("Retry map") }
        }
        if (error != null) Text("Map unavailable. Run tracking still works.", style = MaterialTheme.typography.bodySmall)
    }
}

private class RunMapHolder(val view: MapView, private val live: Boolean, private val onError: (String) -> Unit) {
    private var map: MapLibreMap? = null
    private var points: List<RunPoint> = emptyList()
    private var position: RunPoint? = null
    private var dark = false
    private var loadedDark: Boolean? = null
    private var framed = false
    private var lastPosition: RunPoint? = null
    private var renderedPoints: List<RunPoint>? = null
    private var renderedPosition: RunPoint? = null
    var following = live

    fun attach(map: MapLibreMap) {
        this.map = map
        view.addOnDidFailLoadingMapListener { onError(it) }
        map.uiSettings.isAttributionEnabled = true
        map.uiSettings.isLogoEnabled = false
        map.addOnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) following = false
        }
        reload()
    }
    fun update(points: List<RunPoint>, position: RunPoint?, dark: Boolean) {
        this.points = points; this.position = position; this.dark = dark
        if (map == null) return
        if (loadedDark != dark) reload() else draw()
    }
    fun reload() {
        val map = map ?: return
        loadedDark = dark
        // Keep attribution supplied by the provider's style visible.
        map.setStyle(if (dark) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/liberty") { style ->
            renderedPoints = null
            renderedPosition = null
            style.addSource(GeoJsonSource("run-route", routeJson()))
            style.addLayer(LineLayer("run-route-line", "run-route").withProperties(lineColor("#EC4899"), lineWidth(5f)))
            style.addSource(GeoJsonSource("run-position", positionJson()))
            style.addLayer(CircleLayer("run-position-dot", "run-position").withProperties(
                circleColor("#AD1457"), circleRadius(7f), circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)))
            draw()
        }
    }
    fun recenter() {
        following = live
        framed = false
        lastPosition = null
        draw()
    }
    private fun draw() {
        val map = map ?: return
        map.getStyle { style: Style ->
            val target = position ?: points.lastOrNull()
            if (renderedPoints !== points) {
                style.getSourceAs<GeoJsonSource>("run-route")?.setGeoJson(routeJson())
                renderedPoints = points
            }
            if (renderedPosition != target) {
                style.getSourceAs<GeoJsonSource>("run-position")?.setGeoJson(positionJson())
                renderedPosition = target
            }
            if (!live && !framed && points.isNotEmpty()) {
                val coordinates = points.map { LatLng(it.latitude, it.longitude) }.distinct()
                if (coordinates.size > 1) map.moveCamera(CameraUpdateFactory.newLatLngBounds(
                    LatLngBounds.Builder().includes(coordinates).build(), 40))
                else map.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates.first(), 16.0))
                framed = true
            } else if (live && target != null && (!framed || following && target != lastPosition)) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), 16.0))
                framed = true; lastPosition = target
            }
        }
    }
    private fun routeJson(): String {
        val lines = JSONArray()
        points.groupBy { it.segment }.values.filter { it.size >= 2 }.forEach { segment ->
            lines.put(JSONArray().apply { segment.forEach { put(JSONArray(listOf(it.longitude, it.latitude))) } })
        }
        return JSONObject().put("type", "Feature").put("properties", JSONObject()).put("geometry",
            JSONObject().put("type", "MultiLineString").put("coordinates", lines)).toString()
    }
    private fun positionJson(): String {
        val target = position ?: points.lastOrNull()
        val features = JSONArray()
        if (target != null) features.put(JSONObject().put("type", "Feature").put("properties", JSONObject())
            .put("geometry", JSONObject().put("type", "Point").put("coordinates", JSONArray(listOf(target.longitude, target.latitude)))))
        return JSONObject().put("type", "FeatureCollection").put("features", features).toString()
    }
}
