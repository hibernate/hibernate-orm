/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cascade/circle/MultiPathCircleCascadeDelayedInsert.hbm.xml"
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.GENERATE_STATISTICS, value = "true"),
				@Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0"),
				@Setting(name = Environment.CHECK_NULLABILITY, value = "false")
		}
)
public class MultiPathCircleCascadeCheckNullFalseDelayedInsertTest extends AbstractMultiPathCircleCascadeTest {
}
