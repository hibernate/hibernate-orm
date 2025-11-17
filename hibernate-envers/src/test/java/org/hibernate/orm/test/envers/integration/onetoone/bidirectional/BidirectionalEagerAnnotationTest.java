/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetoone.BidirectionalEagerAnnotationRefEdOneToOne;
import org.hibernate.orm.test.envers.entities.onetoone.BidirectionalEagerAnnotationRefIngOneToOne;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Erik-Berndt Scheper
 */
@JiraKey(value = "HHH-3854")
@EnversTest
@Jpa(annotatedClasses = {
		BidirectionalEagerAnnotationRefEdOneToOne.class,
		BidirectionalEagerAnnotationRefIngOneToOne.class
})
public class BidirectionalEagerAnnotationTest {
	private Integer refIngId1 = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			BidirectionalEagerAnnotationRefEdOneToOne ed1 = new BidirectionalEagerAnnotationRefEdOneToOne();
			BidirectionalEagerAnnotationRefIngOneToOne ing1 = new BidirectionalEagerAnnotationRefIngOneToOne();
			ed1.setData( "referredEntity1" );
			ed1.setRefIng( ing1 );
			ing1.setData( "referringEntity" );
			ing1.setRefedOne( ed1 );
			em.persist( ed1 );
			em.persist( ing1 );

			refIngId1 = ing1.getId();
		} );
	}

	@Test
	public void testNonProxyObjectTraversing(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BidirectionalEagerAnnotationRefIngOneToOne referencing = auditReader.find( BidirectionalEagerAnnotationRefIngOneToOne.class, refIngId1, 1 );
			assertNotNull( referencing.getRefedOne().getData() );
		} );
	}
}
