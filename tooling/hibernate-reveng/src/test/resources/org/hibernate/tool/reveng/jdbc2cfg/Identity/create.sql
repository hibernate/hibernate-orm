CREATE TABLE `autoinc` (`id` IDENTITY NOT NULL,  `data` varchar(100) default NULL,  PRIMARY KEY  (`id`))
CREATE TABLE `noautoinc` (`id` int NOT NULL,  `data` varchar(100) default NULL,  PRIMARY KEY  (`id`))
