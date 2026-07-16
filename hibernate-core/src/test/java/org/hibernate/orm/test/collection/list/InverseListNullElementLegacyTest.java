/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.list;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}, which writes the order column from
 * {@code WriteIndexCoordinatorStandard}.
 *
 * @author Donghwan Kim
 */
@DomainModel(
		annotatedClasses = {
				AbstractInverseListNullElementTest.ListParent.class,
				AbstractInverseListNullElementTest.ListChild.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class InverseListNullElementLegacyTest extends AbstractInverseListNullElementTest {
}
