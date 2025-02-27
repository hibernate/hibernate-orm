/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-19206")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true, extendedEnhancement = true)
public class DirtyTrackingIdTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				MyEntity.class
		};
	}

	@Test
	public void test() {
		inTransaction( session -> {
			MyEntity myEntity = new MyEntity();
			myEntity.setAnId( new MyEntityId( 1L ) );
			myEntity.setData( "initial" );
			session.persist( myEntity );

			// This is unnecessary, but should be harmless...
			// Unfortunately it causes dirty checking to misbehave.
			// Comment it, and the test will pass.
			myEntity.setAnId( new MyEntityId( 1L ) );

			myEntity.setData( "updated" );
		} );
		inTransaction( session -> {
			var entityFromDb = session.find( MyEntity.class, new MyEntityId( 1L ) );
			assertThat( entityFromDb.getData() ).isEqualTo( "updated" );
		} );
	}

	// --- //

	@Entity(name = "MyEntity")
	public static class MyEntity {
		// The name of this property must be (alphabetically) before the name of "data" to trigger the bug.
		// Yes, it's weird.
		@EmbeddedId
		private MyEntityId anId;
		private String data;

		public void setAnId(MyEntityId id) {
			this.anId = id;
		}

		public MyEntityId getAnId() {
			return anId;
		}

		public String getData() {
			return data;
		}

		public void setData(String name) {
			this.data = name;
		}
	}

	@Embeddable
	public static class MyEntityId {
		private Long id;

		public MyEntityId() {
		}

		public MyEntityId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public final boolean equals(Object o) {
			if ( !(o instanceof MyEntityId) ) {
				return false;
			}

			return Objects.equals( id, ( (MyEntityId) o ).id );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( id );
		}
	}
}
