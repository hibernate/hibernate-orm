/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading;

import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = BasicEntity.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16350" )
public class PreLoadEventTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, "test" ) ) );
		scope.getSessionFactory().getEventEngine().getListenerRegistry().appendListeners(
				EventType.PRE_LOAD,
				new AssertingPreLoadEventListener()
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testPreLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BasicEntity be = session.createQuery( "from BasicEntity", BasicEntity.class ).getSingleResult();
			assertThat( be.getData() ).isEqualTo( "test" );
		} );
	}

	public static class AssertingPreLoadEventListener implements PreLoadEventListener {
		@Override
		public void onPreLoad(PreLoadEvent event) {
			event.getPersister().getValues( event.getEntity() );
			assertThat( event.getState() ).isNotNull();
			assertThat( event.getState() ).hasSize( 1 );
			assertThat( event.getState()[0] ).isEqualTo( "test" );
		}
	}
}
