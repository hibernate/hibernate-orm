/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Test for making sure that any set entity entry extra state is propagated from temporary to final entity entries.
 *
 * @author Gunnar Morling
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class, jiraKey = "HHH-9918")
@DomainModel( annotatedClasses = ExtraStateTest.ChineseTakeawayRestaurant.class )
@SessionFactory
public class ExtraStateTest {
	private SessionImplementor sessionRef;

	@Test
	@JiraKey(value = "HHH-9451")
	public void shouldMaintainExtraStateWhenUsingIdentityIdGenerationStrategy(SessionFactoryScope scope) {
		scope.inSession(
				(nonTransactedSession) -> {
					sessionRef = nonTransactedSession;
					final ChineseTakeawayRestaurant persisted = scope.fromTransaction(
							nonTransactedSession,
							(session) -> {
								ChineseTakeawayRestaurant mrKim = new ChineseTakeawayRestaurant();
								mrKim.setGobelinStars( 3 );

								// As a side-effect, the id setter will populate the test extra state
								session.persist( mrKim );

								return mrKim;
							}
					);

					TestExtraState extraState = getEntityEntry( persisted, nonTransactedSession ).getExtraState( TestExtraState.class );
					assertNotNull( extraState, "Test extra state was not propagated from temporary to final entity entry" );
					assertEquals( 311, extraState.getValue() );

					sessionRef = null;
				}
		);
	}

	private EntityEntry getEntityEntry(Object object, SessionImplementor nonTransactedSession) {
		return nonTransactedSession.getPersistenceContext().getEntry( object );
	}

	@SuppressWarnings("unused")
	@Entity
	@Table(name = "ChineseTakeawayRestaurant")
	public class ChineseTakeawayRestaurant {

		private long id;
		private int gobelinStars;

		public ChineseTakeawayRestaurant() {
		}

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public long getId() {
			return id;
		}

		/**
		 * Sets the test extra state as a side effect
		 */
		public void setId(long id) {
			getEntityEntry( this, sessionRef ).addExtraState( new TestExtraState( 311 ) );
			this.id = id;
		}

		public int getGobelinStars() {
			return gobelinStars;
		}

		public void setGobelinStars(int gobelinStars) {
			this.gobelinStars = gobelinStars;
		}
	}

	private static class TestExtraState implements EntityEntryExtraState {

		private final long value;

		public TestExtraState(long value) {
			this.value = value;
		}

		public long getValue() {
			return value;
		}

		@Override
		public void addExtraState(EntityEntryExtraState extraState) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType) {
			throw new UnsupportedOperationException();
		}
	}
}
