/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.env;

import java.util.function.BiConsumer;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

/**
 * @author Steve Ebersole
 */
public final class TestingDatabaseInfo {
	public static volatile String DRIVER = "org.h2.Driver";
	public static volatile String URL = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE";
	public static volatile String USER = "sa";
	public static volatile String PASS = "";

	public static final Dialect DIALECT = new H2Dialect();

	public static void forEachSetting(BiConsumer<String,String> consumer) {
		consumer.accept( JdbcSettings.JAKARTA_JDBC_DRIVER, DRIVER );
		consumer.accept( JdbcSettings.JAKARTA_JDBC_URL, URL );
		consumer.accept( JdbcSettings.JAKARTA_JDBC_USER, USER );
		consumer.accept( JdbcSettings.JAKARTA_JDBC_PASSWORD, PASS );
		consumer.accept( JdbcSettings.DIALECT, DIALECT.getClass().getName() );
	}
	public static Configuration buildBaseConfiguration() {
		final Configuration configuration = new Configuration();
		forEachSetting( configuration::setProperty );
		return configuration;
	}

	private TestingDatabaseInfo() {
	}
}
