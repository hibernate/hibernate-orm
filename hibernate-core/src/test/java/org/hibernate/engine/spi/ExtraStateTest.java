/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.engine.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test for making sure that any set entity entry extra state is propagated from temporary to final entity entries.
 *
 * @author Gunnar Morling
 */
public class ExtraStateTest extends BaseCoreFunctionalTestCase {

	/**
	 * Storing it as a field so it can be accessed from the entity setter.
	 */
	private Session session;

	@Test
	@TestForIssue(jiraKey = "HHH-9451")
	public void shouldMaintainExtraStateWhenUsingIdentityIdGenerationStrategy() {
		session = openSession();
		session.getTransaction().begin();

		ChineseTakeawayRestaurant mrKim = new ChineseTakeawayRestaurant();
		mrKim.setGobelinStars( 3 );

		// As a side-effect, the id setter will populate the test extra state
		session.persist( mrKim );

		session.getTransaction().commit();

		TestExtraState extraState = getEntityEntry( mrKim ).getExtraState( TestExtraState.class );
		assertNotNull( "Test extra state was not propagated from temporary to final entity entry", extraState );
		assertEquals( 311, extraState.getValue() );

		session.close();
	}

	private EntityEntry getEntityEntry(Object object) {
		return ( (SessionImpl) session ).getPersistenceContext().getEntry( object );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ChineseTakeawayRestaurant.class };
	}

	@Entity
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
			getEntityEntry( this ).addExtraState( new TestExtraState( 311 ) );
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
