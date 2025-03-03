/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.foreign;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12738")
@Jpa(
		annotatedClasses = {
				ForeignGeneratorResourceLocalTest.Contract.class,
				ForeignGeneratorResourceLocalTest.Customer.class,
				ForeignGeneratorResourceLocalTest.CustomerContractRelation.class
		},
		integrationSettings = {
				@Setting(
						name = AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY,
						value = "jta"
				),
				@Setting(
						name = AvailableSettings.JTA_PLATFORM,
						value = "org.hibernate.testing.jta.TestingJtaPlatformImpl"
				),
		}
)
public class ForeignGeneratorJtaTest extends ForeignGeneratorResourceLocalTest {
}
