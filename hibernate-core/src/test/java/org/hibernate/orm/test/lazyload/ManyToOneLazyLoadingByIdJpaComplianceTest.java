/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lazyload;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.JPA_PROXY_COMPLIANCE, value = "true")
)
public class ManyToOneLazyLoadingByIdJpaComplianceTest extends ManyToOneLazyLoadingByIdTest {

	@Override
	protected void assertProxyState(Continent continent) {
		assertEquals( "Europe", continent.getName() );
	}
}
