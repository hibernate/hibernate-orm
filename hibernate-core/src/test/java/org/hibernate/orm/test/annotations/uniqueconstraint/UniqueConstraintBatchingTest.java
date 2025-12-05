/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import jakarta.persistence.PersistenceException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.spi.SQLExceptionLogging;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12688")
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = {
				Room.class,
				Building.class,
				House.class
		},
		integrationSettings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5")
)
public class UniqueConstraintBatchingTest {
	@RegisterExtension
	public LoggerInspectionExtension logInspection =
			LoggerInspectionExtension.builder().setLogger( SQLExceptionLogging.ERROR_LOG ).build();

	private Triggerable triggerable;

	@BeforeEach
	public void setUp() {
		triggerable = logInspection.watchForLogMessages( "Unique index" );
		triggerable.reset();
	}

	@Test
	public void testBatching(EntityManagerFactoryScope scope) throws Exception {
		Room livingRoom = new Room();

		scope.inTransaction(
				entityManager -> {
					livingRoom.setId( 1L );
					livingRoom.setName( "livingRoom" );
					entityManager.persist( livingRoom );
				} );

		scope.inTransaction(
				entityManager -> {
					House house = new House();
					house.setId( 1L );
					house.setCost( 100 );
					house.setHeight( 1000L );
					house.setRoom( livingRoom );
					entityManager.persist( house );
				} );

		try {
			scope.inTransaction(
					entityManager -> {
						House house2 = new House();
						house2.setId( 2L );
						house2.setCost( 100 );
						house2.setHeight( 1001L );
						house2.setRoom( livingRoom );
						entityManager.persist( house2 );
					} );
			fail( "Should throw exception" );
		}
		catch (PersistenceException e) {
			assertEquals( 1, triggerable.triggerMessages().size() );
			assertTrue( triggerable.triggerMessage().startsWith( "Unique index or primary key violation" ) );
		}
	}

}
