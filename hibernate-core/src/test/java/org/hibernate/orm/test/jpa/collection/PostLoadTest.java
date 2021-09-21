/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.collection;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-6043")
@Jpa(
		annotatedClasses = { Child.class, Parent.class }
)
public class PostLoadTest {

	/**
	 * Load an entity with a collection of associated entities, that uses a @PostLoad method to
	 * access the association.
	 */
	@Test
	public void testAccessAssociatedSetInPostLoad(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Child child = new Child();
						child.setId( 1 );
						Parent daddy = new Parent();
						daddy.setId( 1 );
						child.setDaddy( daddy );
						Set<Child> children = new HashSet<>();
						children.add( child );
						daddy.setChildren( children );

						entityManager.getTransaction().begin();
						entityManager.persist( daddy );
						entityManager.getTransaction().commit();
						entityManager.clear();

						daddy = entityManager.find( Parent.class, 1 );
						assertEquals( 1, daddy.getNrOfChildren() );
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

}
