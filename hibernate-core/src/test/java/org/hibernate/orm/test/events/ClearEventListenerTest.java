/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel
@SessionFactory
@BootstrapServiceRegistry(integrators = ClearEventListenerTest.CustomLoadIntegrator.class)
public class ClearEventListenerTest {
	@Test
	public void testExplicitClear(SessionFactoryScope scope) {
		LISTENER.callCount = 0;

		scope.inSession(
				session -> {
					session.clear();
					assertThat( LISTENER.callCount ).isEqualTo( 1 );
				}
		);
		assertThat( LISTENER.callCount ).isEqualTo( 1 );
	}

	@Test
	public void testAutoClear(SessionFactoryScope scope) {
		LISTENER.callCount = 0;

		scope.inSession(
				s -> {
					var session = s.sessionWithOptions().autoClear(true).openSession();
					session.getTransaction().begin();
					try {
						assertThat( LISTENER.callCount ).isEqualTo( 0 );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						session.getTransaction().rollback();
						throw e;
					}
					assertThat( LISTENER.callCount ).isEqualTo( 1 );
				}
		);

		assertThat( LISTENER.callCount ).isEqualTo( 1 );
	}

	private static final TheListener LISTENER = new TheListener();

	private static class TheListener implements ClearEventListener {
		private int callCount;

		@Override
		public void onClear(ClearEvent event) {
			callCount++;
		}
	}

	public static class CustomLoadIntegrator implements Integrator {
		@Override
		public void integrate(
				Metadata metadata,
				BootstrapContext bootstrapContext,
				SessionFactoryImplementor sessionFactory) {
			integrate( sessionFactory );
		}

		private void integrate(SessionFactoryImplementor sessionFactory) {
			sessionFactory.getEventListenerRegistry().setListeners(
					EventType.CLEAR,
					LISTENER
			);
		}
	}
}
