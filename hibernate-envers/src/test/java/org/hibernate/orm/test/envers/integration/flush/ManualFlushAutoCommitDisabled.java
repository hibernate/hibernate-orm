/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7017")
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class},
		integrationSettings = @Setting(name = JdbcSettings.AUTOCOMMIT, value = "false"))
public class ManualFlushAutoCommitDisabled extends ManualFlush {
}
