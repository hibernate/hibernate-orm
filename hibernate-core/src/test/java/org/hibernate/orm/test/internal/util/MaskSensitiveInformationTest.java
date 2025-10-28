/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.internal.util;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that sensitive information is correctly masked.
 *
 * @author Bruno P. Kinoshita
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa
public class MaskSensitiveInformationTest {
	private static final String EXPECTED_MASKED_VALUE = "****";

	@Test
	public void testMaskOutSensitiveInformation(EntityManagerFactoryScope scope) {
		Map<String, Object> properties = scope.getEntityManagerFactory().getProperties();
		assertThat( properties.get( AvailableSettings.USER ) ).isEqualTo( EXPECTED_MASKED_VALUE );
		assertThat( properties.get( AvailableSettings.PASS ) ).isEqualTo( EXPECTED_MASKED_VALUE );
		assertThat( properties.get( AvailableSettings.JAKARTA_JDBC_USER ) ).isEqualTo( EXPECTED_MASKED_VALUE );
		assertThat( properties.get( AvailableSettings.JAKARTA_JDBC_PASSWORD ) ).isEqualTo( EXPECTED_MASKED_VALUE );
		assertThat( properties.get( AvailableSettings.JPA_JDBC_USER ) ).isEqualTo( EXPECTED_MASKED_VALUE );
		assertThat( properties.get( AvailableSettings.JPA_JDBC_PASSWORD ) ).isEqualTo( EXPECTED_MASKED_VALUE );
	}
}
