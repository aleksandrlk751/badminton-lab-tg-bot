package ru.badmintonlab.worker.http;

/**
 * Раздел справочника игроков на badminton4u: одиночный (дефолт сайта) или парный.
 */
public enum PlayerDirectoryListType {

    /** {@code players/?sex_m=1} — одиночный рейтинг (type=s по умолчанию). */
    SINGLES("s"),

    /** {@code players/?type=d&sex_m=1} — парный рейтинг. */
    DOUBLES("d");

    private final String siteType;

    PlayerDirectoryListType(String siteType) {
        this.siteType = siteType;
    }

    public String siteType() {
        return siteType;
    }
}
