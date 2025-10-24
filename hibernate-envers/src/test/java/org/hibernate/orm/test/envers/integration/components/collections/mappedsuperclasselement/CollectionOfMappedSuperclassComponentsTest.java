/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.collections.mappedsuperclasselement;

import java.util.Arrays;
import java.util.Set;

import org.hibernate.envers.AuditReaderFactory;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@JiraKey( value = "HHH-9193" )
@EnversTest
@Jpa(annotatedClasses = {MappedSuperclassComponentSetTestEntity.class, Code.class})
public class CollectionOfMappedSuperclassComponentsTest {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1
			em.getTransaction().begin();
			MappedSuperclassComponentSetTestEntity cte1 = new MappedSuperclassComponentSetTestEntity();
			em.persist( cte1 );
			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();
			cte1 = em.find( MappedSuperclassComponentSetTestEntity.class, cte1.getId() );
			cte1.getComps().add( new Code( 1 ) );
			cte1.getCompsNotAudited().add( new Code( 100 ) );
			em.getTransaction().commit();

			id1 = cte1.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals(
					Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( MappedSuperclassComponentSetTestEntity.class, id1 )
			);
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9193")
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			MappedSuperclassComponentSetTestEntity entity = auditReader.find(
					MappedSuperclassComponentSetTestEntity.class,
					id1,
					1
			);
			assertEquals( 0, entity.getComps().size() );
			assertEquals( 0, entity.getCompsNotAudited().size() );

			entity = auditReader.find( MappedSuperclassComponentSetTestEntity.class, id1, 2 );

			// TODO: what is the expectation here? The collection is audited, but the embeddable class
			// has no data and it extends a mapped-superclass that is not audited.
			// currently the collection has 1 element that has value AbstractCode.UNDEFINED
			// (which seems wrong). I changed the expected size to 0 which currently fails; is that what
			// should be expected?
			Set<Code> comps1 = entity.getComps();
			assertEquals( 0, comps1.size() );

			// The contents of entity.getCompsNotAudited() is unspecified, so no need to test.
		} );
	}
}
