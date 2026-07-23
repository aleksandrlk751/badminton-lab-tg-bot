package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.badmintonlab.core.entity.Player;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findBySexIsNull();

    /**
     * Нечёткий поиск игрока по нику или ФИО (от 3 символов).
     * Подстрочный ILIKE по нику/фамилии/имени/«Фамилия Имя» (использует gin_trgm_ops индексы),
     * сортировка по триграммной близости — ближайшие совпадения выше.
     */
    @Query(value = """
            SELECT * FROM player p
            WHERE p.nick ILIKE ('%' || :q || '%')
               OR p.last_name ILIKE ('%' || :q || '%')
               OR p.first_name ILIKE ('%' || :q || '%')
               OR (COALESCE(p.last_name, '') || ' ' || COALESCE(p.first_name, '')) ILIKE ('%' || :q || '%')
            ORDER BY GREATEST(
                similarity(p.nick, :q),
                similarity(COALESCE(p.last_name, ''), :q),
                similarity(COALESCE(p.first_name, ''), :q)
            ) DESC, p.nick ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Player> search(@Param("q") String q, @Param("limit") int limit);
}
