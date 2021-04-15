CREATE SCHEMA IF NOT EXISTS `paint_fun`;
CREATE TABLE IF NOT EXISTS `paint_fun`.`users` (
                                       `login` varchar(36) NOT NULL,
                                       `name` text NOT NULL,
                                       `password_hash` text NOT NULL,
                                       PRIMARY KEY (`login`)
);

CREATE TABLE IF NOT EXISTS `paint_fun`.`snapshots` (
                                        `source_board_id` varchar(36) NOT NULL,
                                        `name` varchar(36) NOT NULL,
                                        `data` text NOT NULL,
                                        PRIMARY KEY (`whiteboard_id`, `name`),
                                        FOREIGN KEY (`user_login`) REFERENCES `paint_fun`.`users`(`login`)
);

CREATE TABLE IF NOT EXISTS `paint_fun`.`snapshots_restore_links` (
                                        `whiteboard_id` varchar(36) NOT NULL,
                                        `snapshot_name` varchar(36) NOT NULL,
                                        `snapshot_from` varchar(36) NOT NULL,
                                        PRIMARY KEY (`whiteboard_id`),
                                        FOREIGN KEY (`snapshot_name`, `snapshot_from`) REFERENCES `paint_fun`.`snapshots`(`name`, `whiteboard_id`)
);
