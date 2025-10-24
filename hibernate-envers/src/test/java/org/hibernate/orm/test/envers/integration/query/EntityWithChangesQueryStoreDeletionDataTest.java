/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;


import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Chris Cranford
 */
@Jpa(annotatedClasses = {
		AbstractEntityWithChangesQueryTest.Simple.class
}, integrationSettings = {
		@Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		@Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true")
})
@EnversTest
@JiraKey(value = "HHH-8058")
public class EntityWithChangesQueryStoreDeletionDataTest extends AbstractEntityWithChangesQueryTest {
}
