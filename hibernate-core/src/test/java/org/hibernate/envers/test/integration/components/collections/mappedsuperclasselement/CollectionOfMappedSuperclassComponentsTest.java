/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.collections.mappedsuperclasselement;

import java.util.Arrays;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9193" )
public class CollectionOfMappedSuperclassComponentsTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MappedSuperclassComponentSetTestEntity.class, Code.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		MappedSuperclassComponentSetTestEntity cte1 = new MappedSuperclassComponentSetTestEntity();

		em.persist( cte1 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( MappedSuperclassComponentSetTestEntity.class, cte1.getId() );

		cte1.getComps().add( new Code( 1 ) );
		cte1.getCompsNotAudited().add( new Code( 100 ) );

		em.getTransaction().commit();

		id1 = cte1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals(
				Arrays.asList( 1, 2 ),
				getAuditReader().getRevisions( MappedSuperclassComponentSetTestEntity.class, id1 )
		);
	}

	@Test
	public void testHistoryOfId1() {
		MappedSuperclassComponentSetTestEntity entity = getAuditReader().find(
				MappedSuperclassComponentSetTestEntity.class,
				id1,
				1
		);
		assertEquals( 0, entity.getComps().size() );
		assertEquals( 0, entity.getCompsNotAudited().size() );

		entity = getAuditReader().find( MappedSuperclassComponentSetTestEntity.class, id1, 2 );

		// The collection is audited and while the embeddable has no data and extended a non-audited
		// mapped superclass, the collection will hold 1 element where the state of each element in
		// the collection is that all properties will have their default values.
		Set<Code> comps1 = entity.getComps();
		assertEquals( 1, comps1.size() );
		assertEquals( AbstractCode.UNDEFINED, comps1.iterator().next().getCode() );

		// The contents of entity.getCompsNotAudited() is unspecified, so no need to test.
	}
}