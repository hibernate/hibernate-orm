/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test database info logging on bootstrap
 *
 * @implSpec This is limited to H2 so that we know what to expect from the logging
 *
 * @author Steve Ebersole
 */
@MessageKeyInspection(
		messageKey = "HHH10001005",
		logger = @Logger( loggerName = ConnectionInfoLogger.LOGGER_NAME )
)
public class BootLoggingTests {
	@Test
	@ServiceRegistry( settings = @Setting( name = AvailableSettings.ALLOW_METADATA_ON_BOOT, value = "false" ) )
	@DomainModel( standardModels = StandardDomainModel.HELPDESK )
	@SessionFactory( exportSchema = false, generateStatistics = true )
	@RequiresDialect( H2Dialect.class )
	void testNoJdbcAccess(MessageKeyWatcher dbInfoLoggingWatcher, ServiceRegistryScope registryScope, SessionFactoryScope sessionFactoryScope) {
		// make sure we get the db-info logged
		assertThat( dbInfoLoggingWatcher.wasTriggered() ).isTrue();

		// make sure it was logged as we expect
		final ConfigurationService configurationService = registryScope.getRegistry().requireService( ConfigurationService.class );
		assertThat( dbInfoLoggingWatcher.getTriggeredMessages() ).hasSize( 1 );
		final String loggedMessage = dbInfoLoggingWatcher.getTriggeredMessages().get( 0 );
		assertThat( loggedMessage ).contains( "Database JDBC URL [" + configurationService.getSettings().get( JdbcSettings.URL ) );
		assertThat( loggedMessage ).contains( "Database driver: " + configurationService.getSettings().get( JdbcSettings.DRIVER ) );
		assertThat( loggedMessage ).contains( "Maximum pool size: " + configurationService.getSettings().get( JdbcSettings.POOL_SIZE ) );

		// and make sure we did not connect to the database
		final StatisticsImplementor statistics = sessionFactoryScope.getSessionFactory().getStatistics();
		assertThat( statistics.getConnectCount() ).isEqualTo( 0 );
	}
}
