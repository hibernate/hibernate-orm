/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.exception;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;

@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = {
				AbstractEntityInsertUniqueConstraintBatchingTest.UniqueEntity.class,
				AbstractEntityInsertUniqueConstraintBatchingTest.CollectionOwner.class
		},
		integrationSettings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5")
)
public class EntityInsertUniqueConstraintBatchingTest extends AbstractEntityInsertUniqueConstraintBatchingTest {
}
