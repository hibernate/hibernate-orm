/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.readwriteexpression;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnversTest
@Jpa(annotatedClasses = {Staff.class})
public class ReadWriteExpressionChange {

	private static final Double HEIGHT_INCHES = 73.0d;
	private static final Double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;

	private Integer id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id = scope.fromTransaction( em -> {
			Staff staff = new Staff( HEIGHT_INCHES, 1 );
			em.persist( staff );
			em.flush();
			return staff.getId();
		} );
	}

	@Test
	public void shouldRespectWriteExpression(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var resultList = em.createNativeQuery( "select size_in_cm from t_staff_AUD where id =" + id )
					.getResultList();
			assertEquals( 1, resultList.size() );
			Double sizeInCm = (Double) resultList.get( 0 );
			assertEquals( HEIGHT_CENTIMETERS, sizeInCm.doubleValue(), 0.00000001 );
		} );
	}

	@Test
	public void shouldRespectReadExpression(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List<Number> revisions = auditReader.getRevisions( Staff.class, id );
			assertEquals( 1, revisions.size() );
			Number number = revisions.get( 0 );
			Staff staffRev = auditReader.find( Staff.class, id, number );
			assertEquals( HEIGHT_INCHES, staffRev.getSizeInInches(), 0.00000001 );
		} );
	}
}
