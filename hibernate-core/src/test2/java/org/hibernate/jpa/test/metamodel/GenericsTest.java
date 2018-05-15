/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;

import static org.junit.Assert.*;

/**
 * @author Christian Beikov
 */
@TestForIssue( jiraKey = "HHH-11540" )
public class GenericsTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				PersonId.class
		};
	}

	@Test
	public void testEmbeddableTypeExists() {
		EntityManager em = getOrCreateEntityManager();
		EmbeddableType<PersonId> idType = em.getMetamodel().embeddable( PersonId.class) ;
		assertNotNull( idType );
		em.close();
	}

}
