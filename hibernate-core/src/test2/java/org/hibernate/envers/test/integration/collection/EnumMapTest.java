/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.EnumMapEntity;
import org.hibernate.envers.test.entities.collection.EnumMapType;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-6374")
public class EnumMapTest extends BaseEnversJPAFunctionalTestCase {
	private Integer entityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				EnumMapEntity.class,
				EnumMapType.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getOrCreateEntityManager();
		try {
			// revision 1
			EnumMapEntity entity = new EnumMapEntity();
			entity.getTypes().put( EnumMapEntity.EnumType.TYPE_A, new EnumMapType( "A" ) );
			entity.getTypes().put( EnumMapEntity.EnumType.TYPE_B, new EnumMapType( "B" ) );
			em.getTransaction().begin();
			em.persist( entity );
			em.getTransaction().commit();
			// revision 2
			em.getTransaction().begin();
			entity = em.find( EnumMapEntity.class, entity.getId() );
			entity.getTypes().remove( EnumMapEntity.EnumType.TYPE_A );
			entity.getTypes().put( EnumMapEntity.EnumType.TYPE_C, new EnumMapType( "C" ) );
			em.getTransaction().commit();
			entityId = entity.getId();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testRevisionsCount() {
		assertEquals( 2, getAuditReader().getRevisions( EnumMapEntity.class, entityId ).size() );
	}

	@Test
	public void testAuditEnumMapCollection() {
		EnumMapEntity rev1 = getAuditReader().find( EnumMapEntity.class, entityId, 1 );
		assertTrue( rev1.getTypes().keySet().containsAll(
				Arrays.asList( EnumMapEntity.EnumType.TYPE_A, EnumMapEntity.EnumType.TYPE_B )
		) );
		EnumMapEntity rev2 = getAuditReader().find( EnumMapEntity.class, entityId, 2 );
		assertTrue( rev2.getTypes().keySet().containsAll(
				Arrays.asList( EnumMapEntity.EnumType.TYPE_B, EnumMapEntity.EnumType.TYPE_C )
		) );
	}

}
