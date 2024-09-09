/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.metamodel.EmbeddableType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Christian Beikov
 */
@JiraKey( value = "HHH-11540" )
@Jpa(annotatedClasses = {
		Person.class,
		PersonId.class
})
public class GenericsTest {

	@Test
	public void testEmbeddableTypeExists(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					EmbeddableType<PersonId> idType = entityManager.getMetamodel().embeddable( PersonId.class) ;
					assertNotNull( idType );
				}
		);
	}
}
