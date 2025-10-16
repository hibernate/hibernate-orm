/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {Country.class})
@EnversTest
public class ImmutableClassAccessType {
	private Country country;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			country = Country.of( 123, "Germany" );
			em.persist( country );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals(
					Arrays.asList( 1 ),
					AuditReaderFactory.get( em ).getRevisions( Country.class, country.getCode() )
			);
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Country country1 = em.find( Country.class, country.getCode() );
			assertEquals( country1, country );

			Country history = AuditReaderFactory.get( em ).find( Country.class, country1.getCode(), 1 );
			assertEquals( country, history );
		} );
	}
}
