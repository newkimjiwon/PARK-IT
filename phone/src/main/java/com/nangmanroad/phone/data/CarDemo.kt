package com.nangmanroad.phone.data

/**
 * The 내 차량 tab is a concept: no public Pleos SDK lets a phone read or control the car remotely
 * (Vehicle Control SDK is "준비 중" as of 2026-11). Every value here is sample data and every
 * command is simulated, and the screen says so.
 */
enum class CarCommand(
    val title: String,
    val question: String,
    val detail: String,
    val successMessage: String,
) {
    LOCK("도어 잠그기", "차량 도어를 잠그시겠습니까?", "모든 도어를 잠가요.", "도어를 잠갔어요"),
    UNLOCK("도어 열기", "차량 도어 잠금을 해제하시겠습니까?", "주변에 아무도 없는지 확인하세요.", "도어 잠금을 해제했어요"),
    CLIMATE_ON("공조 켜기", "공조를 켜시겠어요?", "목표 온도로 실내를 맞춰요. 차량 상태에 따라 시간이 걸릴 수 있어요.", "공조를 켰어요"),
    CLIMATE_OFF("공조 끄기", "공조를 끄시겠어요?", "실내 공조를 멈춰요.", "공조를 껐어요"),
    CLOSE_WINDOWS("창문 닫기", "열린 창문을 닫으시겠어요?", "끼임 방지 기능이 있는 차량에서만 지원돼요.", "창문을 닫았어요"),
}

data class CarDemoState(
    val model: String = "Hyundai IONIQ 5",
    val nickname: String = "우리 집 아이오닉",
    val parked: Boolean = true,
    val batteryPercent: Int = 72,
    val rangeKm: Int = 310,
    val charging: Boolean = false,
    val doorsLocked: Boolean = true,
    val driverWindowOpen: Boolean = true,
    val cabinTemp: Int = 26,
    val climateOn: Boolean = false,
    val targetTemp: Int = 22,
    val moodLightSupported: Boolean = false,
    /** Waiting for the user to confirm in a dialog. */
    val pending: CarCommand? = null,
    /** Sent and waiting for the (simulated) car to answer. */
    val processing: CarCommand? = null,
) {
    fun apply(command: CarCommand): CarDemoState = when (command) {
        CarCommand.LOCK -> copy(doorsLocked = true)
        CarCommand.UNLOCK -> copy(doorsLocked = false)
        CarCommand.CLIMATE_ON -> copy(climateOn = true, cabinTemp = targetTemp)
        CarCommand.CLIMATE_OFF -> copy(climateOn = false)
        CarCommand.CLOSE_WINDOWS -> copy(driverWindowOpen = false)
    }
}
