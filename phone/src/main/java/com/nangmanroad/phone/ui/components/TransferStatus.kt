package com.nangmanroad.phone.ui.components

import com.nangmanroad.core.sync.CarTransferProgress
import com.nangmanroad.phone.PhoneUiState

/** Waiting has not left the phone or has no car to go to; sent is on the server; done reached the car. */
enum class TransferStage { WAITING, SENT, DONE }

data class TransferStatus(val label: String, val detail: String, val stage: TransferStage, val needsCarLink: Boolean = false)

/**
 * Where a place sent to the car really is. "보냈어요" is only said once it left the phone, and
 * "수신" only once the car said so, so waiting and failure never read as success.
 */
fun PhoneUiState.transferStatus(spotId: String): TransferStatus? {
    val transfer = user.carQueue.firstOrNull { it.spotId == spotId } ?: return null
    return when {
        account.session == null ->
            TransferStatus("전송 대기", "이 휴대폰에만 담겨 있어요. 로그인하면 차량으로 보내져요.", TransferStage.WAITING)
        transfer.carProgress == CarTransferProgress.GUIDANCE_STARTED ->
            TransferStatus("길 안내 시작", "차량에서 이 장소로 길 안내를 시작했어요.", TransferStage.DONE)
        transfer.carProgress == CarTransferProgress.RECEIVED ->
            TransferStatus("차량 수신", "차량 화면의 '휴대폰' 메뉴에 도착했어요.", TransferStage.DONE)
        user.linkedCars == 0 ->
            TransferStatus("전송 대기", "연결된 차량이 없어요. 차량을 연결하면 전달돼요.", TransferStage.WAITING, needsCarLink = true)
        else ->
            TransferStatus("전송 성공 · 수신 대기", "서버에 보냈어요. 차량 앱이 켜져 있으면 곧 받아요.", TransferStage.SENT)
    }
}
