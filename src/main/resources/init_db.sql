CREATE SCHEMA IF NOT EXISTS `paint_fun`;
CREATE TABLE IF NOT EXISTS `paint_fun`.`users` (
                                       `login` varchar(36) NOT NULL,
                                       `name` text NOT NULL,
                                       `password_hash` text NOT NULL,
                                       PRIMARY KEY (`login`)
);

CREATE TABLE IF NOT EXISTS `paint_fun`.`auth_tokens` (
                                        `id` varchar(36) NOT NULL,
                                        `identity` varchar(36) NOT NULL,
                                        `expiry` DATE NOT NULL,
                                        `lastTouched` DATE DEFAULT NULL,
                                        PRIMARY KEY (`id`)
);
