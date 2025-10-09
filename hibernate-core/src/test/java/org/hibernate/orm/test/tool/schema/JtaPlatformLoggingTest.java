/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-12763" )
@MessageKeyInspection(
		messageKey = "HHH000490",
		resetBeforeEach = false,
		logger = @Logger( loggerName = CoreMessageLogger.NAME )
)
@ServiceRegistry(settingConfigurations = @SettingConfiguration( configurer = TestingJtaBootstrap.class ) )
@DomainModel(annotatedClasses = JtaPlatformLoggingTest.TestEntity.class)
public class JtaPlatformLoggingTest {

	@Test
	public void test(MessageKeyWatcher messageKeyWatcher) {
		assertTrue( messageKeyWatcher.wasTriggered() );
		assertThat( messageKeyWatcher.getTriggeredMessages() ).hasSize( 1 );
		assertThat( messageKeyWatcher.getTriggeredMessages().get( 0 ) ).startsWith( "HHH000490: Using JTA platform" );
	}

	@Entity( name = "TestEntity" )
	@Table( name = "TestEntity" )
	public static class TestEntity {
		@Id
		public Integer id;
		String name;
	}
}
