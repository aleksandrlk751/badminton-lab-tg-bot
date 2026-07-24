package ru.badmintonlab.core.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface JointPartnershipView {
    Long getPartnerId();

    Instant getTournamentStartsAt();

    BigDecimal getUserDelta();

    BigDecimal getPartnerDelta();
}
