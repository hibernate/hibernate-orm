/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class EmbeddedTypeTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Product.class, ShelfLife.class, VersionedEntity.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-6896" )
	public void ensureComponentsReturnedAsManagedType() {
		ManagedType<ShelfLife> managedType = entityManagerFactory().getMetamodel().managedType( ShelfLife.class );
		// the issue was in regards to throwing an exception, but also check for nullness
		assertNotNull( managedType );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4702" )
	public void testSingularAttributeAccessByName() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		SingularAttribute soldDate_ = em.getMetamodel().embeddable( ShelfLife.class )
				.getSingularAttribute( "soldDate" );
		assertEquals( java.sql.Date.class, soldDate_.getBindableJavaType());
		assertEquals( java.sql.Date.class, soldDate_.getType().getJavaType() );
		assertEquals( java.sql.Date.class, soldDate_.getJavaType() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5821" )
	public void testVersionAttributeMetadata() {
		EntityManager em = getOrCreateEntityManager();
		EntityType<VersionedEntity> metadata = em.getMetamodel().entity( VersionedEntity.class );
		assertNotNull( metadata.getDeclaredVersion( int.class ) );
		assertTrue( metadata.getDeclaredVersion( int.class ).isVersion() );
		assertEquals( 3, metadata.getDeclaredSingularAttributes().size() );
		assertTrue( metadata.getDeclaredSingularAttributes().contains( metadata.getDeclaredVersion( int.class ) ) );
		em.close();
	}

}
