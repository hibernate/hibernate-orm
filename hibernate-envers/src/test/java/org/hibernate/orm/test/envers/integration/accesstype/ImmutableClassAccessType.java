/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImmutableClassAccessType extends BaseEnversJPAFunctionalTestCase {
	private Country country;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Country.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		country = Country.of( 123, "Germany" );
		em.persist( country );
		em.getTransaction().commit();

	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 )
				.equals(
						getAuditReader().getRevisions(
								Country.class,
								country.getCode()
						)
				);
	}

	@Test
	public void testHistoryOfId1() {
		Country country1 = getEntityManager().find(
				Country.class,
				country.getCode()
		);
		assertEquals( country1, country );

		Country history = getAuditReader().find( Country.class, country1.getCode(), 1 );
		assertEquals( country, history );
	}

}
