package ru.badmintonlab.core.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Вывод пола по дисциплинам рейтинга, пар или коду категории турнира;
 * fallback по отчеству и типичным русским именам (офлайн, без сайта).
 * Generic D/XD и смешанные категории (X*) не используются; конфликт M+F → {@code null}.
 */
public final class PlayerSexInference {

    private static final Set<Discipline> MALE_DISCIPLINES = EnumSet.of(Discipline.MS, Discipline.MD);
    private static final Set<Discipline> FEMALE_DISCIPLINES = EnumSet.of(Discipline.WS, Discipline.WD);

    private static final Set<String> MALE_FIRST_NAMES = Set.of(
            "Алексей", "Александр", "Андрей", "Анзор", "Антон", "Артём", "Артем", "Аслонбек",
            "Борис", "Вадим", "Венер", "Владимир", "Владислав", "Глеб", "Григорий", "Дамир",
            "Даниэле", "Данил", "Данила", "Данило", "Денис", "Дмитрий", "Евгений", "Егор",
            "Иван", "Игорь", "Илья", "Кирилл", "Константин", "Максим", "Матвей", "Михаил",
            "Никита", "Николай", "Олег", "Павел", "Пётр", "Петр", "Рафаэль", "Роман", "Руслан",
            "Семен", "Семён", "Сергей", "Станислав", "Степан", "Тимофей", "Фёдор",
            "Федор", "Юрий");

    private static final Set<String> FEMALE_FIRST_NAMES = Set.of(
            "Алина", "Алия", "Анастасия", "Анна", "Арина", "Валентина", "Валерия", "Вера",
            "Вероника", "Виктория", "Виолетта", "Галина", "Глафира", "Дарья", "Диана", "Евгения",
            "Екатерина", "Елена", "Елизавета", "Эльвира", "Жанна", "Ирина", "Кристина", "Ксения", "Лариса",
            "Людмила", "Маргарита", "Марина", "Мария", "Наталия", "Наталья", "Оксана", "Ольга",
            "Полина", "Светлана", "София", "Татьяна", "Ульяна", "Юлия");

    private PlayerSexInference() {
    }

    public static PlayerSex inferFromDisciplines(Collection<Discipline> disciplines) {
        boolean male = disciplines.stream().anyMatch(MALE_DISCIPLINES::contains);
        boolean female = disciplines.stream().anyMatch(FEMALE_DISCIPLINES::contains);
        if (male && !female) {
            return PlayerSex.M;
        }
        if (female && !male) {
            return PlayerSex.F;
        }
        return null;
    }

    public static PlayerSex inferFromCategoryCodes(Collection<String> categoryCodes) {
        boolean male = false;
        boolean female = false;
        for (String code : categoryCodes) {
            PlayerSex sex = sexFromCategoryCode(code);
            if (sex == PlayerSex.M) {
                male = true;
            } else if (sex == PlayerSex.F) {
                female = true;
            }
        }
        if (male && !female) {
            return PlayerSex.M;
        }
        if (female && !male) {
            return PlayerSex.F;
        }
        return null;
    }

    /**
     * Fallback по ФИО: отчество (приоритет), затем имя; при {@code first_name} пустом — пробуем {@code last_name}.
     * Конфликт отчества и имени → {@code null}. Работает офлайн по полям {@code player}.
     */
    public static PlayerSex inferFromName(String firstName, String lastName, String patronymic) {
        PlayerSex fromPatronymic = inferFromPatronymic(patronymic);
        String nameToken = normalizeNameToken(firstName);
        if (nameToken == null) {
            nameToken = normalizeNameToken(lastName);
        }
        PlayerSex fromName = nameToken != null ? inferFromFirstName(nameToken) : null;
        if (fromPatronymic != null && fromName != null && fromPatronymic != fromName) {
            return null;
        }
        if (fromPatronymic != null) {
            return fromPatronymic;
        }
        return fromName;
    }

    static PlayerSex inferFromPatronymic(String patronymic) {
        String token = normalizeNameToken(patronymic);
        if (token == null) {
            return null;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.endsWith("овна") || lower.endsWith("евна") || lower.endsWith("ична")
                || lower.endsWith("инична") || lower.endsWith("кызы")) {
            return PlayerSex.F;
        }
        if (lower.endsWith("ович") || lower.endsWith("евич") || lower.endsWith("оглы")) {
            return PlayerSex.M;
        }
        if (lower.endsWith("ич")) {
            return PlayerSex.M;
        }
        return null;
    }

    static PlayerSex inferFromFirstName(String firstName) {
        String token = normalizeNameToken(firstName);
        if (token == null) {
            return null;
        }
        if (MALE_FIRST_NAMES.contains(token)) {
            return PlayerSex.M;
        }
        if (FEMALE_FIRST_NAMES.contains(token)) {
            return PlayerSex.F;
        }
        return null;
    }

    static String normalizeNameToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.chars().anyMatch(Character::isLetter)) {
            return null;
        }
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT)
                + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Префикс кода категории: MS/MD → M, WS/WD → F; X* и generic D — не используются.
     */
    static PlayerSex sexFromCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        String upper = categoryCode.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("X")) {
            return null;
        }
        if (upper.startsWith("WD") || upper.startsWith("WS")) {
            return PlayerSex.F;
        }
        if (upper.startsWith("MD") || upper.startsWith("MS")) {
            return PlayerSex.M;
        }
        return null;
    }
}
