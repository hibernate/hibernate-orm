/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;


/**
 * Tests merging multiple detached representations of the same entity when explicitly disallowed.
 *
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9106")
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "disallow")
)
public class MergeMultipleEntityCopiesDisallowedTest extends MergeMultipleEntityCopiesDisallowedByDefaultTest {
}
