CREATE SCHEMA IF NOT EXISTS `paint_fun`;
CREATE TABLE IF NOT EXISTS `paint_fun`.`users` (
                                       `id` serial,
                                       `login` varchar(36) NOT NULL,
                                       `name` text NOT NULL,
                                       PRIMARY KEY (`id`)
);
