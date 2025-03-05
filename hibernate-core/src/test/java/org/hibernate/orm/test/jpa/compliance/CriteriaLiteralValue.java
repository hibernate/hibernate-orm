/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;

import static org.junit.jupiter.api.Assertions.fail;

@Jpa(
		properties = @Setting(name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true")
)
public class CriteriaLiteralValue {

	@Test
	public void testLiteralValueCannotBeNull(EntityManagerFactoryScope scope) {

		final CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();
		try {
			builder.literal(null);
			fail( "TCK expects an IllegalArgumentException" );
		} catch (IllegalArgumentException iae) {
//			//expected by TCK
		}
	}

}
