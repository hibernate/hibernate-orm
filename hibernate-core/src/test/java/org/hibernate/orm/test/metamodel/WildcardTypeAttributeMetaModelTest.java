/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.metamodel;

import jakarta.persistence.metamodel.EntityType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.orm.test.metamodel.wildcardmodel.AbstractEntity;
import org.hibernate.orm.test.metamodel.wildcardmodel.AbstractOwner;
import org.hibernate.orm.test.metamodel.wildcardmodel.EntityOne;
import org.hibernate.orm.test.metamodel.wildcardmodel.OwnerOne;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = {
				AbstractEntity.class,
				AbstractOwner.class,
				EntityOne.class,
				OwnerOne.class
		}
)
public class WildcardTypeAttributeMetaModelTest {

	@Test
	@JiraKey(value = "HHH-9403")
	public void testWildcardGenericAttributeCanBeResolved(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			EntityType<AbstractOwner> entity = entityManager.getMetamodel().entity( AbstractOwner.class );
			assertNotNull( entity );
		} );
	}

}
