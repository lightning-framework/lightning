/**
 * Use UTF8 encoding.
 */
SET NAMES utf8;
SET CHARACTER SET utf8;
SET character_set_connection = utf8;
SET collation_connection = utf8_general_ci;

/**
 * Delete any copies of the tables already in the database.
 */
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS user_privileges;
DROP TABLE IF EXISTS files;
DROP TABLE IF EXISTS sessions;
DROP TABLE IF EXISTS recovery_tokens;
DROP TABLE IF EXISTS email_verification_tokens;
DROP TABLE IF EXISTS auth_tokens;
DROP TABLE IF EXISTS auth_attempts;
DROP TABLE IF EXISTS auth_sessions;
DROP TABLE IF EXISTS groups;
DROP TABLE IF EXISTS group_membership;
DROP TABLE IF EXISTS group_privileges;
SET FOREIGN_KEY_CHECKS = 1;

/**
 * Create the schema.
 */
CREATE TABLE users (
  id int(64) unsigned not null auto_increment,
  username varchar(255) not null,
  email varchar(255) not null,
  email_verified tinyint(1) UNSIGNED NOT NULL DEFAULT 0,
  encrypted_password varchar(255) not null,
  properties mediumblob default null,
  banned_until int(64) unsigned not null default 0,
  secret_key varchar(255) not null,
  PRIMARY KEY(id),
  UNIQUE(username),
  UNIQUE(email),
  UNIQUE(secret_key)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE user_privileges (
  user_id int(64) UNSIGNED NOT NULL,
  privilege_id int(64) UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, privilege_id),
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE files (
  id int(64) unsigned not null auto_increment,
  name varchar(255) not null,
  content LONGBLOB not null,
  last_modified int(64) unsigned not null,
  PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE sessions (
  session_id varchar(255) not null,
  last_save int(64) unsigned not null,
  data mediumblob not null,
  PRIMARY KEY(session_id)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE recovery_tokens (
  token_hash varchar(255) NOT NULL,
  user_id int(64) UNSIGNED NOT NULL,
  origin_ip varchar(255) NOT NULL,
  expires int(64) UNSIGNED NOT NULL,
  PRIMARY KEY (token_hash),
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE(token_hash),
  INDEX(user_id)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE email_verification_tokens (
  token_hash varchar(255) NOT NULL,
  user_id int(64) UNSIGNED NOT NULL,
  email varchar(255) NOT NULL,
  origin_ip varchar(255) NOT NULL,
  expires int(64) UNSIGNED NOT NULL,
  PRIMARY KEY (token_hash),
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE(token_hash),
  UNIQUE(user_id)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE auth_tokens (
  user_id int(64) UNSIGNED NOT NULL,
  token_hash varchar(255) NOT NULL,
  expires int(64) UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, token_hash),
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE(token_hash)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE auth_sessions (
  user_id int(64) UNSIGNED NOT NULL,
  token_hash varchar(255) NOT NULL,
  expires int(64) UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, token_hash),
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE(token_hash)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE auth_attempts (
  ipaddress varchar(255) NOT NULL,
  user_id int(64) UNSIGNED DEFAULT NULL,
  time int(64) UNSIGNED NOT NULL,
  successful tinyint(1) UNSIGNED NOT NULL,
  fraudulent tinyint(1) UNSIGNED NOT NULL,
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE groups (
  id int(64) UNSIGNED NOT NULL auto_increment,
  name varchar(64) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (name)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE group_membership (
  group_id int(64) UNSIGNED NOT NULL,
  user_id int(64) UNSIGNED NOT NULL,
  PRIMARY KEY (group_id, user_id),
  FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE,
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE group_privileges (
  group_id int(64) UNSIGNED NOT NULL,
  privilege_id int(64) UNSIGNED NOT NULL,
  PRIMARY KEY (group_id, privilege_id),
  FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
) CHARACTER SET utf8 COLLATE utf8_general_ci;
