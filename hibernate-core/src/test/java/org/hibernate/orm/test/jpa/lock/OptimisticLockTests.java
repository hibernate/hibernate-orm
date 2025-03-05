/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.LockModeType;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = Lockable.class)
@SessionFactory(useCollectingStatementInspector = true)
public class OptimisticLockTests {
	@Test
	@JiraKey(value = "HHH-9419")
	public void testNoVersionCheckAfterRemove(SessionFactoryScope sessions) {
		final SQLStatementInspector sqlCollector = sessions.getCollectingStatementInspector();
		sqlCollector.clear();

		final Lockable created = sessions.fromTransaction( (session) -> {
			final Lockable entity = new Lockable( "name" );
			session.persist( entity );
			return entity;
		} );
		assertThat( created.getVersion() ).isEqualTo( 0 );

		final Lockable locked = sessions.fromTransaction( (session) -> {
			final ActionQueue actionQueue = session.unwrap( SessionImplementor.class ).getActionQueue();
			assertThat( actionQueue.hasBeforeTransactionActions() ).isFalse();

			final Lockable loaded = session.createQuery( "from Lockable", Lockable.class )
					.setLockMode( LockModeType.OPTIMISTIC )
					.getSingleResult();
			assertThat( loaded.getVersion() ).isEqualTo( 0 );
			assertThat( actionQueue.hasBeforeTransactionActions() ).isTrue();

			sqlCollector.clear();
			session.remove( loaded );

			return loaded;
		} );

		assertThat( locked.getVersion() ).isEqualTo( 0 );

		// should be just the deletion
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).startsWith( "delete from Lockable " );
	}
}
