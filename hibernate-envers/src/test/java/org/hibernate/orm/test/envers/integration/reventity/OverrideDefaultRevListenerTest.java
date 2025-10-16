/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6696")
@EnversTest
@Jpa(
		annotatedClasses = {StrTestEntity.class, LongRevNumberRevEntity.class},
		integrationSettings = @Setting(name = EnversSettings.REVISION_LISTENER,
				value = "org.hibernate.orm.test.envers.integration.reventity.TestRevisionListener")
)
public class OverrideDefaultRevListenerTest extends GloballyConfiguredRevListenerTest {
}
