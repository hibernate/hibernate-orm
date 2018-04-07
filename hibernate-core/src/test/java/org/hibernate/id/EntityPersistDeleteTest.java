/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.FlushMode;
import org.hibernate.action.internal.EntityAction;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertTrue;

@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@TestForIssue(jiraKey = "HHH-12464")
public class EntityPersistDeleteTest extends BaseCoreFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
		Logger.getMessageLogger( CoreMessageLogger.class, EntityAction.class.getName() )
	);

	@Test
	public void test() {
		Triggerable triggerable = logInspection.watchForLogMessages( "The Persistence Context does not contain any entry" );
		triggerable.reset();

		doInHibernate( this::sessionFactory, session -> {
			session.setHibernateFlushMode( FlushMode.COMMIT );
			RootEntity entity = new RootEntity();
			session.persist( entity );
			session.delete( entity );
		} );

		assertTrue( triggerable.wasTriggered() );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				RootEntity.class,
				RelatedEntity.class,
		};
	}
}
