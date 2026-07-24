package ru.badmintonlab.core.repository.projection;

import ru.badmintonlab.core.domain.PlayerSex;

import java.math.BigDecimal;

public interface PartnerCandidateView {
    Long getPlayerId();

    String getNick();

    String getLastName();

    String getFirstName();

    String getPatronymic();

    String getCity();

    BigDecimal getRating();

    PlayerSex getSex();
}
