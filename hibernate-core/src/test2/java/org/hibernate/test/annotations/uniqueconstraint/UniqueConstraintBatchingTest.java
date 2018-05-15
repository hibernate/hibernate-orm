/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.uniqueconstraint;

import java.util.Map;
import javax.persistence.PersistenceException;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12688")
@RequiresDialect(H2Dialect.class)
public class UniqueConstraintBatchingTest extends BaseEntityManagerFunctionalTestCase {

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Room.class,
				Building.class,
				House.class
		};
	}

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, SqlExceptionHelper.class.getName() ) );

	private Triggerable triggerable;

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.STATEMENT_BATCH_SIZE, 5 );
		triggerable = logInspection.watchForLogMessages( "Unique index" );
		triggerable.reset();
	}

	@Test
	public void testBatching() throws Exception {
		Room livingRoom = new Room();

		doInJPA( this::entityManagerFactory, entityManager -> {
			livingRoom.setId( 1l );
			livingRoom.setName( "livingRoom" );
			entityManager.persist( livingRoom );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			House house = new House();
			house.setId( 1l );
			house.setCost( 100 );
			house.setHeight( 1000l );
			house.setRoom( livingRoom );
			entityManager.persist( house );
		} );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				House house2 = new House();
				house2.setId( 2l );
				house2.setCost( 100 );
				house2.setHeight( 1001l );
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
