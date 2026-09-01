package blash10x.kis.ota.domain;

/**
 * 사다리 간격에 곱하는 가중치를 계산한다. 종목마다 변동성 성향에 맞춰 사다리를 넓히거나 좁히는 몫이다.
 *
 * <p><b>계약:</b> 반환값은 1.0 근처로 정규화되어야 한다. {@link LadderPricer} 의 손실 회피 가드가
 * {@code rate < 5.0 * weight} 로 가중치가 1 스케일이라는 전제에 기대기 때문이다. 스케일이 크게 다른 값을 내면 사다리 간격(①)은
 * 그럭저럭 동작해도 손실 회피(②)가 조용히 어긋난다.
 */
@FunctionalInterface
public interface LadderWeight {

  double of(LadderInput input);
}
