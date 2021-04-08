CREATE SCHEMA IF NOT EXISTS `paint_fun`;
CREATE TABLE IF NOT EXISTS `paint_fun`.`users` (
                                       `login` varchar(36) NOT NULL,
                                       `name` text NOT NULL,
                                       `password_hash` text NOT NULL,
                                       PRIMARY KEY (`login`)
);

CREATE TABLE IF NOT EXISTS `paint_fun`.`snapshots` (
                                        `name` text NOT NULL,
                                        `user` varchar(36) NOT NULL,
                                        `data` text NOT NULL,
                                        PRIMARY KEY (`name`),
                                        FOREIGN KEY (user) REFERENCES paint_fun.users(login)
);
