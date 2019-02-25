CREATE SEQUENCE user_info_id_seq START WITH 1001;
CREATE TABLE user_info (
  id                      BIGINT PRIMARY KEY DEFAULT nextval('user_info_id_seq'),
  user_name               VARCHAR(100)   NOT   NULL,
  head_img                VARCHAR(500)   NOT   NULL,
  gender                  INT            NOT   NULL
);
ALTER SEQUENCE user_info_id_seq OWNED BY user_info.id;

ALTER TABLE user_info ADD password VARCHAR(100)   DEFAULT '' NOT NULL;
ALTER TABLE user_info ADD forbidden BOOLEAN   DEFAULT FALSE NOT NULL;


CREATE TABLE user_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT(1001, 1),
  username VARCHAR(100) NOT NULL,
  head_img VARCHAR(500) NOT NULL,
  gender INT NOT NULL
);
-- ALTER SEQUENCE user_info_id_seq2 OWNED BY user_info.id;

ALTER TABLE user_info ADD password VARCHAR(100)   DEFAULT '' NOT NULL;
ALTER TABLE user_info ADD forbidden BOOLEAN   DEFAULT FALSE NOT NULL;


