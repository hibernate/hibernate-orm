/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.metamodel;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Jpa(
		annotatedClasses = {
				ProductEntity.class,
				Person.class,
				Company.class
		}
)
public class EmbeddableMetaModelTest {

	@Test
	@JiraKey(value = "HHH-11111")
	public void testEmbeddableCanBeResolvedWhenUsedAsInterface(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertNotNull( entityManager.getMetamodel().embeddable( LocalizedValue.class ) );
			assertEquals( LocalizedValue.class, ProductEntity_.description.getElementType().getJavaType() );
			assertNotNull( LocalizedValue_.value );
		} );
	}


	@Test
	@JiraKey(value = "HHH-12124")
	public void testEmbeddableEquality(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertTrue( entityManager.getMetamodel().getEmbeddables().contains( Company_.address.getType() ) );
			assertTrue( entityManager.getMetamodel().getEmbeddables().contains( Person_.address.getType() ) );
		} );
	}
}
