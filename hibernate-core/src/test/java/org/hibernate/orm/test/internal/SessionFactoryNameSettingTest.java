/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.internal;

import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hibernate.orm.test.internal.SessionFactoryNameSettingTest.SESSION_FACTORY_NAME;

@Jpa(
		integrationSettings = @Setting(name = AvailableSettings.SESSION_FACTORY_NAME, value = SESSION_FACTORY_NAME)
)
@JiraKey("HHH-19072")
public class SessionFactoryNameSettingTest {
	public static final String SESSION_FACTORY_NAME = "TEST_SESSION_FACTORY";

	@Test
	public void testSessionFactoryNameSettingInfluencingSessionFactoryName(EntityManagerFactoryScope scope) {
		assertThat( scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getName() )
				.isEqualTo( SESSION_FACTORY_NAME );
	}

	@Test
	public void testSessionFactoryNameSettingInfluencingSessionFactoryJndiName(EntityManagerFactoryScope scope) {
		Assertions.assertThat( scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getJndiName() )
				.isEqualTo( SESSION_FACTORY_NAME );
	}
}
