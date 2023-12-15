/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.metamodel.genericmodel;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Yanming Zhou
 */
@Jpa(
		annotatedClasses = {
				GenericMappedSuperclassMetamodelTest.Book.class,
				GenericMappedSuperclassMetamodelTest.Owner.class,
				GenericMappedSuperclassMetamodelTest.OwnerContainer.class
		}
)
public class GenericMappedSuperclassMetamodelTest {

	@Test
	public void testGenericAttributeFromMappedSuperclassIsNotTypeErased(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			EntityType<Book> bookType = entityManager.getMetamodel().entity( Book.class );
			assertEquals( Owner.class, bookType.getAttribute("owner").getJavaType() );
		});
	}

	@Entity
	public static class Book extends OwnerContainer<Owner> {

		@Id
		private Long id;
	}

	@Entity
	public static class Owner {
		@Id
		private Long id;
	}

	@MappedSuperclass
	public static class OwnerContainer<T> {

		@ManyToOne
		T owner;
	}
}
