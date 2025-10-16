/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {SetRefEdEntity.class, SetRefIngEntity.class})
public class InverseSideChanges {
	private Integer ed1_id;
	private Integer ing1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
		SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( ed1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SetRefEdEntity ed = em.find( SetRefEdEntity.class, ed1.getId() );
			em.persist( ing1 );
			ed.setReffering( new HashSet<SetRefIngEntity>() );
			ed.getReffering().add( ing1 );
		} );

		ed1_id = ed1.getId();
		ing1_id = ing1.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( SetRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( SetRefIngEntity.class, ing1_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity rev1 = auditReader.find( SetRefEdEntity.class, ed1_id, 1 );
			assertEquals( Collections.EMPTY_SET, rev1.getReffering() );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			SetRefIngEntity rev2 = auditReader.find( SetRefIngEntity.class, ing1_id, 2 );
			assertEquals( null, rev2.getReference() );
		} );
	}
}
