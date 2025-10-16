/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetoone.BidirectionalEagerHbmRefEdPK;
import org.hibernate.orm.test.envers.entities.onetoone.BidirectionalEagerHbmRefIngPK;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Erik-Berndt Scheper, Amar Singh
 */
@JiraKey(value = "HHH-3854")
@EnversTest
@Jpa(xmlMappings = "mappings/oneToOne/bidirectional/eagerLoading.hbm.xml")
public class BidirectionalEagerHbmTest {
	private Long refIngId1 = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			BidirectionalEagerHbmRefEdPK ed1 = new BidirectionalEagerHbmRefEdPK( "data_ed_1" );
			BidirectionalEagerHbmRefIngPK ing1 = new BidirectionalEagerHbmRefIngPK( "data_ing_1" );
			ing1.setReference( ed1 );
			em.persist( ed1 );
			em.persist( ing1 );

			refIngId1 = ing1.getId();
		} );
	}

	@Test
	public void testNonProxyObjectTraversing(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BidirectionalEagerHbmRefIngPK referencing =
					auditReader.find( BidirectionalEagerHbmRefIngPK.class, refIngId1, 1 );
			assertNotNull( referencing.getReference().getData() );
		} );
	}
}
