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
public class GenericsTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10690" )
	public void testEmbeddableTypeSpecializedByTypeVariable() {
		EntityManager em = getOrCreateEntityManager();
		EntityType<Person> metadata = em.getMetamodel().entity( Person.class );
		EmbeddableType<PersonId> idMetadata = em.getMetamodel().embeddable( PersonId.class) ;
		assertNotNull( idMetadata );
		assertEquals( idMetadata, metadata.getIdType() );
		em.close();
	}

	@Test
	public void testBasicTypeSpecializedByTypeVariable() {
		EntityManager em = getOrCreateEntityManager();
		EntityType<Person> metadata = em.getMetamodel().entity( Person.class );
		assertEquals( String.class, metadata.getAttribute( "data" ).getJavaType() );
		em.close();
	}

	@Test
	public void testEntityTypeSpecializedByTypeVariable() {
		EntityManager em = getOrCreateEntityManager();
		EntityType<Person> metadata = em.getMetamodel().entity( Person.class );
		assertEquals( Person.class, metadata.getAttribute( "entity" ).getJavaType() );
		em.close();
	}

}
