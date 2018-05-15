/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7478")
public class RegisterUserEventListenerTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class };
	}

	@DynamicTest
	public void testTransactionProcessSynchronization() {
		final CountingPostInsertTransactionBoundaryListener listener =
				new CountingPostInsertTransactionBoundaryListener();

		sessionFactoryScope().getSessionFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.POST_INSERT )
				.appendListener( listener );

		doInHibernate( this::sessionFactory, session -> {
			final StrTestEntity entity = new StrTestEntity( "str1" );
			session.save( entity );
		} );

		assertThat( listener.getBeforeCounter(), is( 3 ) );
		assertThat( listener.getAfterCounter(), is( 3 ) );
	}

	private static class CountingPostInsertTransactionBoundaryListener implements PostInsertEventListener {
		private int beforeCounter = 0;
		private int afterCounter = 0;

		@Override
		public void onPostInsert(PostInsertEvent event) {
			event.getSession().getActionQueue().registerProcess( session -> ++beforeCounter );
			event.getSession().getActionQueue().registerProcess( (success, session) -> ++afterCounter );
		}

		@Override
		public boolean requiresPostCommitHandling(EntityTypeDescriptor persister) {
			return true;
		}

		public int getBeforeCounter() {
			return beforeCounter;
		}

		public int getAfterCounter() {
			return afterCounter;
		}
	}
}
