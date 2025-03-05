/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests merging multiple detached representations of the same entity when explicitly allowed and logged.
 *
 * @author Gail Badner
 */
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "log")
)
public class MergeMultipleEntityCopiesAllowedLoggedTest extends MergeMultipleEntityCopiesAllowedTest {
}
