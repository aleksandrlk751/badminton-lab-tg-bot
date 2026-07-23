-- Пол игрока: справочник players/?sex_m=1 / sex_f=1 (этап 5).
CREATE TYPE player_sex AS ENUM ('M', 'F');

ALTER TABLE player ADD COLUMN sex player_sex;

CREATE INDEX idx_player_sex ON player (sex) WHERE sex IS NOT NULL;
