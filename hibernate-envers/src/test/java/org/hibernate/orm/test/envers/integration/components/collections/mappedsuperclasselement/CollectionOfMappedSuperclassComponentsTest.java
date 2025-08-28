/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.collections.mappedsuperclasselement;

import java.util.Arrays;
import java.util.Set;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
@JiraKey( value = "HHH-9193" )
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
	@FailureExpected( jiraKey = "HHH-9193")
	public void testHistoryOfId1() {
		MappedSuperclassComponentSetTestEntity entity = getAuditReader().find(
				MappedSuperclassComponentSetTestEntity.class,
				id1,
				1
		);
		assertEquals( 0, entity.getComps().size() );
		assertEquals( 0, entity.getCompsNotAudited().size() );

		entity = getAuditReader().find( MappedSuperclassComponentSetTestEntity.class, id1, 2 );

		// TODO: what is the expectation here? The collection is audited, but the embeddable class
		// has no data and it extends a mapped-superclass that is not audited.
		// currently the collection has 1 element that has value AbstractCode.UNDEFINED
		// (which seems wrong). I changed the expected size to 0 which currently fails; is that what
		// should be expected?
		Set<Code> comps1 = entity.getComps();
		assertEquals( 0, comps1.size() );

		// The contents of entity.getCompsNotAudited() is unspecified, so no need to test.
	}
}
