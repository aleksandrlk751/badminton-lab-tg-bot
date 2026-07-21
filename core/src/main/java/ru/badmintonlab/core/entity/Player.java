package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "player")
public class Player {

    @Id
    private Long id;

    @Column(nullable = false, length = 128)
    private String nick;

    @Column(name = "first_name", length = 128)
    private String firstName;

    @Column(name = "last_name", length = 128)
    private String lastName;

    @Column(length = 128)
    private String patronymic;

    @Column(length = 128)
    private String city;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "playing_hand", length = 16)
    private String playingHand;

    @Column(name = "hall_id")
    private Long hallId;

    @Column(name = "registered_at")
    private LocalDate registeredAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Player() {
    }

    public Long getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPlayingHand() {
        return playingHand;
    }

    public Long getHallId() {
        return hallId;
    }

    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
