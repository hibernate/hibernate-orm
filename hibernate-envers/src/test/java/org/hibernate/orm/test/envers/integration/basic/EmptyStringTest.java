/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7246")
@RequiresDialect(OracleDialect.class)
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class})
public class EmptyStringTest {
	private Integer emptyId = null;
	private Integer nullId = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			StrTestEntity emptyEntity = new StrTestEntity( "" );
			em.persist( emptyEntity );
			StrTestEntity nullEntity = new StrTestEntity( null );
			em.persist( nullEntity );

			emptyId = emptyEntity.getId();
			nullId = nullEntity.getId();
		} );

		scope.inTransaction( em -> {
			// Should not generate revision after NULL to "" modification and vice versa on Oracle.
			StrTestEntity emptyEntity = em.find( StrTestEntity.class, emptyId );
			emptyEntity.setStr( null );
			em.merge( emptyEntity );
			StrTestEntity nullEntity = em.find( StrTestEntity.class, nullId );
			nullEntity.setStr( "" );
			em.merge( nullEntity );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, emptyId ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, nullId ) );
		} );
	}
}
