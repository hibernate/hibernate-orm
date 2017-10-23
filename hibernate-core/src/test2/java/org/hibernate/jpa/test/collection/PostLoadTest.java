/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.collection;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue( jiraKey="HHH-6043" )
public class PostLoadTest extends BaseEntityManagerFunctionalTestCase {

    /**
     * Load an entity with a collection of associated entities, that uses a @PostLoad method to
     * access the association.
     */
	@Test
    public void testAccessAssociatedSetInPostLoad() {
    	Child child = new Child();
        child.setId(1);
        Parent daddy = new Parent();
        daddy.setId(1);
        child.setDaddy(daddy);
        Set<Child> children = new HashSet<Child>();
        children.add(child);
        daddy.setChildren(children);

        EntityManager em = getOrCreateEntityManager();

        em.getTransaction().begin();
        em.persist(daddy);
        em.getTransaction().commit();
        em.clear();

        daddy = em.find(Parent.class, 1);
        assertEquals(1, daddy.getNrOfChildren());
    }
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Child.class, Parent.class };
	}
}


