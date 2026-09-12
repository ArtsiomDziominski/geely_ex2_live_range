package com.geely.ex2.range.data.vhal

object VhalIds {
    const val GLOBAL_AREA_ID = 0
    const val CAR_MANAGER_PROPERTY = "property"

    const val PROP_ED_EV_BATTERY_PERCENTAGE = 0x2140A6ED
    const val PROP_EV_BATTERY_LEVEL = 0x11600309
    const val PROP_EV_CURRENT_BATTERY_CAPACITY = 0x1160030D
    const val PROP_INFO_EV_BATTERY_CAPACITY = 0x11600106
    const val PROP_PERF_VEHICLE_SPEED = 0x11600207
    const val PROP_PERF_ODOMETER = 0x11600204
    /** AOSP spec: FLOAT, GLOBAL, unit METER. Not km like PERF_ODOMETER — сверить на авто, см. «Справка». */
    const val PROP_RANGE_REMAINING = 0x11600308
    const val PROP_CURRENT_GEAR = 0x11400401
    const val PROP_GEAR_SELECTION = 0x11400400
    const val PROP_AC_AMBIENT_TEMP = 0x2140A377
    const val PROP_ENV_OUTSIDE_TEMPERATURE = 0x11600703
    const val PROP_PEPS_POWER_MODE = 0x2140A331
    const val PROP_DM_FUNC_DRIVE_MODE_SELECT = 0x22010100
    const val PROP_SETTING_FUNC_ENERGY_REGENERATION = 0x20020500

    /**
     * OEM enums for real charge state — found by decompiling a reference OEM battery app
     * pulled from this same head unit (com.timhss.geelybattery, package com.timhss.capyenergy).
     * That app tries an ECarX-adapter-resolved id first (0x24205C00 / 0x24130200) and falls
     * back to these raw ones when the adapter is unavailable — we only have the raw path.
     * Read as Int, area GLOBAL. Known string values (exact int-per-value not yet confirmed —
     * see raw lines "CHARGING_DISCHARGING_STATE" / "CHARGING_PLUG_STATE" and sample on a real charge):
     *  - DISCHARGING_STATE: No charging / Discharging / AC charging / DC charging /
     *    Charging end / Charging complete / AC charging suspend / DC charging end /
     *    Heating / Booking
     *  - PLUG_STATE: No plug / AC plug connected / DC plug connected /
     *    Integration plug connected / Discharge plug connected
     */
    const val PROP_CHARGING_DISCHARGING_STATE = 0x2140737E
    const val PROP_CHARGING_PLUG_STATE = 0x21407289

    val REGEN_AREAS: IntArray = intArrayOf(0, 1)
    val GEAR_CANDIDATES: IntArray = intArrayOf(PROP_CURRENT_GEAR, PROP_GEAR_SELECTION)

    const val SPEED_RATE_HZ = 1f
    const val ON_CHANGE_HZ = 0f
}
