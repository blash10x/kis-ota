package blash10x.kis.ota.domain;

/**
 * 사다리 한 단. 전송 여부와 무관한 계산 결과다.
 *
 * @param unitPrice 호가단위까지 맞춘 주문 단가
 * @param rate 현재가 대비 등락률. 매도는 양수, 매수는 음수다.
 */
public record LadderOrder(int unitPrice, double rate) {}
