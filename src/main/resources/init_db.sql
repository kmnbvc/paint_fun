CREATE SCHEMA IF NOT EXISTS `paint_fun`;
CREATE TABLE IF NOT EXISTS `paint_fun`.`users` (
                                       `login` varchar(36) NOT NULL,
                                       `name` text NOT NULL,
                                       `password_hash` text NOT NULL,
                                       PRIMARY KEY (`login`)
);

CREATE TABLE IF NOT EXISTS `paint_fun`.`snapshots` (
                                        `source_board_id` uuid NOT NULL,
                                        `name` varchar(36) NOT NULL,
                                        `data` text NOT NULL,
                                        PRIMARY KEY (`source_board_id`, `name`)
);

CREATE TABLE IF NOT EXISTS `paint_fun`.`whiteboards` (
                                        `whiteboard_id` uuid NOT NULL,
                                        `snapshot_name` varchar(36) NOT NULL,
                                        `snapshot_source_id` uuid NOT NULL,
                                        PRIMARY KEY (`whiteboard_id`),
                                        FOREIGN KEY (`snapshot_name`, `snapshot_source_id`) REFERENCES `paint_fun`.`snapshots`(`name`, `source_board_id`)
);

CREATE TYPE `paint_fun`.`access_type_enum` AS ENUM('owner_only', 'anyone');

CREATE TABLE IF NOT EXISTS `paint_fun`.`whiteboards_access` (
                                        `whiteboard_id` uuid NOT NULL,
                                        `whiteboard_owner` varchar(36) NOT NULL,
                                        `access_type` `paint_fun`.`access_type_enum` NOT NULL,
                                        PRIMARY KEY (`whiteboard_id`),
                                        FOREIGN KEY (`whiteboard_owner`) REFERENCES `paint_fun`.`users`(`login`)
);
