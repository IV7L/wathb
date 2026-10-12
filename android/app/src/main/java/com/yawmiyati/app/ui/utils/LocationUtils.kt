package com.yawmiyati.app.ui.utils

data class CityLatLng(val lat: Double, val lng: Double)

private val CITY_COORDS: Map<String, CityLatLng> = mapOf(
    "غزة"         to CityLatLng(31.5017, 34.4668),
    "مدينة غزة"   to CityLatLng(31.5017, 34.4668),
    "خانيونس"     to CityLatLng(31.3428, 34.3064),
    "خان يونس"    to CityLatLng(31.3428, 34.3064),
    "رفح"         to CityLatLng(31.2912, 34.2498),
    "جباليا"      to CityLatLng(31.5279, 34.4913),
    "جبالية"      to CityLatLng(31.5279, 34.4913),
    "دير البلح"   to CityLatLng(31.4176, 34.3478),
    "بيت لاهيا"   to CityLatLng(31.5543, 34.5003),
    "بيت حانون"   to CityLatLng(31.5381, 34.5320),
    "النصيرات"    to CityLatLng(31.4390, 34.3710),
    "البريج"      to CityLatLng(31.4560, 34.3793),
    "المغازي"     to CityLatLng(31.4640, 34.3680),
    "رام الله"    to CityLatLng(31.9038, 35.2034),
    "القدس"       to CityLatLng(31.7683, 35.2137),
    "الخليل"      to CityLatLng(31.5326, 35.0998),
    "نابلس"       to CityLatLng(32.2211, 35.2544),
    "جنين"        to CityLatLng(32.4633, 35.2962),
    "طولكرم"      to CityLatLng(32.3100, 35.0289),
    "قلقيلية"     to CityLatLng(32.1876, 34.9704),
    "بيت لحم"     to CityLatLng(31.7054, 35.2024),
    "أريحا"       to CityLatLng(31.8579, 35.4612),
    "طوباس"       to CityLatLng(32.3205, 35.3677),
    "سلفيت"       to CityLatLng(32.0842, 35.1829),
)

fun cityLatLng(city: String): CityLatLng {
    CITY_COORDS[city]?.let { return it }
    for ((key, coords) in CITY_COORDS) {
        if (city.contains(key) || key.contains(city)) return coords
    }
    return CityLatLng(31.5017, 34.4668) // default: central Gaza
}
