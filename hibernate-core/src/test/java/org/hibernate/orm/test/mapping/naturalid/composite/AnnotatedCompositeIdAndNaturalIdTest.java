/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.composite;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;

/**
 * @author Donnchadh O Donnabhain
 */
@ServiceRegistry(
		settings = {
				@Setting(name = USE_SECOND_LEVEL_CACHE, value = "false"),
				@Setting(name = USE_QUERY_CACHE, value = "false"),
				@Setting(name = GENERATE_STATISTICS, value = "false")
		}
)
@DomainModel(annotatedClasses = {Account.class, AccountId.class})
@SessionFactory
public class AnnotatedCompositeIdAndNaturalIdTest extends AbstractCompositeIdAndNaturalIdTest {
}
