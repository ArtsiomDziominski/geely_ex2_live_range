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
     * Зарядка — id и единицы из Geely EX2 Tools (`ChargingReader`). FUNC_* — функции Flyme:
     * адаптер ГУ сам пересчитывает значение в кВт / минуты / В / А. PROP_* — сырые свойства
     * VHAL с тем же смыслом, запасной путь. Декодирование — `ChargingDecoder`.
     */
    const val FUNC_CHARGING_PLUG_STATE = 0x24130200
    const val PROP_OBC_STS_CC = 0x2140A800
    const val FUNC_CHARGING_AC_POWER = 0x24205400
    const val PROP_CHARGING_AC_POWER = 0x2140A6B3
    const val FUNC_CHARGING_DC_POWER = 0x24207100
    const val PROP_CHARGING_DC_POWER = 0x2140A6B4
    const val FUNC_CHARGING_TIME_LEFT = 0x24140300
    const val PROP_CHARGING_FINISH_TIME = 0x2140A6B6
    const val FUNC_CHARGING_INPUT_VOLTS = 0x24140100
    const val FUNC_CHARGING_INPUT_AMPS = 0x24140200
    /** CHANGING_VCU_StsChg: 0 — нет, 1 — идёт зарядка, 2 — пауза/конец. Пока только в «Справке». */
    const val PROP_VCU_CHARGE_STATUS = 0x2140A6B1

    val REGEN_AREAS: IntArray = intArrayOf(0, 1)
    val GEAR_CANDIDATES: IntArray = intArrayOf(PROP_CURRENT_GEAR, PROP_GEAR_SELECTION)

    const val SPEED_RATE_HZ = 1f
    const val ON_CHANGE_HZ = 0f
}
