/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement.recreate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

@RunWith(BytecodeEnhancerRunner.class) // Pointless here, but necessary to reproduce the problem.
@EnhancementOptions(lazyLoading = true)
public class BytecodeEnhancementElementCollectionRecreateTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				MyEntity.class
		};
	}

	@Test
	public void testRecreateCollection() {
		inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two" ) );
			session.persist( entity );
		} );

		inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			entity.setElements( Arrays.asList( "two", "three" ) );
			session.persist( entity );
		} );

		inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.containsExactlyInAnyOrder( "two", "three" );
		} );
	}

	@Entity(name = "myentity")
	public static class MyEntity {
		@Id
		private Integer id;

		@ElementCollection
		@OrderColumn
		private List<String> elements;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<String> getElements() {
			return elements;
		}

		public void setElements(List<String> elements) {
			this.elements = elements;
		}

	}

}
