/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;

import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */

@Jpa(
		annotatedClasses = {
				Competitor.class,
				Race.class,
				Mail.class
		},
		integrationSettings = {@Setting(name = JpaComplianceSettings.JPA_LOAD_BY_ID_COMPLIANCE, value = "true")}
)
public class GetReferenceTest {
	@Test
	public void testWrongIdType(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getReference( Competitor.class, "30" );
						fail("Expected IllegalArgumentException");
					}
					catch (IllegalArgumentException e) {
						//success
					}
					catch ( Exception e ) {
						fail("Wrong exception: " + e );
					}

					try {
						entityManager.getReference( Mail.class, 1 );
						fail("Expected IllegalArgumentException");
					}
					catch (IllegalArgumentException e) {
						//success
					}
					catch ( Exception e ) {
						fail("Wrong exception: " + e );
					}
				}
		);
	}
	@Test
	public void testWrongIdTypeFind(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.find( Competitor.class, "30" );
						fail("Expected IllegalArgumentException");
					}
					catch (IllegalArgumentException e) {
						//success
					}
					catch ( Exception e ) {
						fail("Wrong exception: " + e );
					}

					try {
						entityManager.find( Mail.class, 1 );
						fail("Expected IllegalArgumentException");
					}
					catch (IllegalArgumentException e) {
						//success
					}
					catch ( Exception e ) {
						fail("Wrong exception: " + e );
					}
				}
		);
	}
}
