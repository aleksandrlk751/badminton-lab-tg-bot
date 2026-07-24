ALTER TABLE tournament
    ADD COLUMN max_player_rating_limit NUMERIC(6, 1);

COMMENT ON COLUMN tournament.max_player_rating_limit IS
    'Макс. рейтинг одного игрока в паре (badminton4u); NULL = без персонального лимита';
