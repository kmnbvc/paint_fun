CREATE SCHEMA IF NOT EXISTS `paint_fun`;
CREATE TABLE IF NOT EXISTS `paint_fun`.`users` (
                                       `login` varchar(36) NOT NULL,
                                       `name` text NOT NULL,
                                       `password_hash` text NOT NULL,
                                       PRIMARY KEY (`login`)
);

CREATE TABLE IF NOT EXISTS `paint_fun`.`snapshots` (
                                        `whiteboard_id` varchar(36) NOT NULL,
                                        `name` varchar(36) NOT NULL,
                                        `user_login` varchar(36) NOT NULL,
                                        `data` text NOT NULL,
                                        PRIMARY KEY (`whiteboard_id`, `name`),
                                        FOREIGN KEY (user_login) REFERENCES paint_fun.users(login)
);
