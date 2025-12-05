/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditedAtSuperclassLevel.auditAllSubclass;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.orm.test.envers.integration.superclass.auditedAtSuperclassLevel.AuditedAllMappedSuperclass;
import org.hibernate.orm.test.envers.integration.superclass.auditedAtSuperclassLevel.NotAuditedSubclassEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 */
@EnversTest
@Jpa(annotatedClasses = {
		AuditedAllMappedSuperclass.class,
		AuditedAllSubclassEntity.class,
		NotAuditedSubclassEntity.class
})
public class MappedSubclassingAllAuditedTest {
	private Integer id2_1;
	private Integer id1_1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			NotAuditedSubclassEntity nas = new NotAuditedSubclassEntity( "nae", "super str", "not audited str" );
			em.persist( nas );
			AuditedAllSubclassEntity ae = new AuditedAllSubclassEntity( "ae", "super str", "audited str" );
			em.persist( ae );
			id1_1 = ae.getId();
			id2_1 = nas.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			AuditedAllSubclassEntity ae = em.find( AuditedAllSubclassEntity.class, id1_1 );
			ae.setStr( "ae new" );
			ae.setSubAuditedStr( "audited str new" );
			NotAuditedSubclassEntity nas = em.find( NotAuditedSubclassEntity.class, id2_1 );
			nas.setStr( "nae new" );
			nas.setNotAuditedStr( "not aud str new" );
		} );
	}

	@Test
	public void testRevisionsCountsForAudited(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( AuditedAllSubclassEntity.class, id1_1 ) );
		} );
	}

	@Test
	public void testRevisionsCountsForNotAudited(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertThrows( NotAuditedException.class, () -> {
				AuditReaderFactory.get( em ).getRevisions( NotAuditedSubclassEntity.class, id2_1 );
			} );
		} );
	}

	@Test
	public void testHistoryOfAudited(EntityManagerFactoryScope scope) {
		AuditedAllSubclassEntity ver1 = new AuditedAllSubclassEntity( id1_1, "ae", "super str", "audited str" );
		AuditedAllSubclassEntity ver2 = new AuditedAllSubclassEntity( id1_1, "ae new", "super str", "audited str new" );

		scope.inEntityManager( em -> {
			AuditedAllSubclassEntity rev1 = AuditReaderFactory.get( em ).find( AuditedAllSubclassEntity.class, id1_1, 1 );
			AuditedAllSubclassEntity rev2 = AuditReaderFactory.get( em ).find( AuditedAllSubclassEntity.class, id1_1, 2 );

			assertNotNull( rev1.getOtherStr() );
			assertNotNull( rev2.getOtherStr() );

			assertEquals( ver1, rev1 );
			assertEquals( ver2, rev2 );
		} );
	}

	@Test
	public void testHistoryOfNotAudited(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertThrows( NotAuditedException.class, () -> {
				AuditReaderFactory.get( em ).find( NotAuditedSubclassEntity.class, id2_1, 1 );
			} );
		} );
	}
}
