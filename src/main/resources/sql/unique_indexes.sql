-- Compatible with MySQL versions that do not support:
-- CREATE UNIQUE INDEX IF NOT EXISTS ...

SET @s1 = (
    SELECT IF(
        (
            SELECT COUNT(1)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'challenge_participation'
              AND index_name = 'uk_challenge_participation_user_challenge'
        ) = 0,
        'ALTER TABLE challenge_participation ADD UNIQUE INDEX uk_challenge_participation_user_challenge (user_id, challenge_id)',
        'SELECT 1'
    )
);
PREPARE stmt1 FROM @s1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @s2 = (
    SELECT IF(
        (
            SELECT COUNT(1)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'spot_favorites'
              AND index_name = 'uk_spot_favorites_user_spot'
        ) = 0,
        'ALTER TABLE spot_favorites ADD UNIQUE INDEX uk_spot_favorites_user_spot (user_id, spot_id)',
        'SELECT 1'
    )
);
PREPARE stmt2 FROM @s2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
