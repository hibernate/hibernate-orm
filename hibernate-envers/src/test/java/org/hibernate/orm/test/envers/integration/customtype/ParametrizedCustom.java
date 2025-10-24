/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.customtype;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.customtype.ParametrizedCustomTypeEntity;

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
@Jpa(annotatedClasses = {ParametrizedCustomTypeEntity.class})
public class ParametrizedCustom {
	private Integer pcte_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (persisting 1 entity)
		this.pcte_id = scope.fromTransaction( em -> {
			ParametrizedCustomTypeEntity pcte = new ParametrizedCustomTypeEntity();
			pcte.setStr( "U" );
			em.persist( pcte );
			return pcte.getId();
		} );

		// Revision 2 (changing the value)
		scope.inTransaction( em -> {
			ParametrizedCustomTypeEntity pcte = em.find( ParametrizedCustomTypeEntity.class, this.pcte_id );
			pcte.setStr( "V" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2 ),
					auditReader.getRevisions( ParametrizedCustomTypeEntity.class, pcte_id )
			);
		} );
	}

	@Test
	public void testHistoryOfCcte(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ParametrizedCustomTypeEntity rev1 = auditReader.find( ParametrizedCustomTypeEntity.class, pcte_id, 1 );
			ParametrizedCustomTypeEntity rev2 = auditReader.find( ParametrizedCustomTypeEntity.class, pcte_id, 2 );

			assertEquals( "xUy", rev1.getStr() );
			assertEquals( "xVy", rev2.getStr() );
		} );
	}
}
