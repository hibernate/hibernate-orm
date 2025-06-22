/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.internal.util.collections.ArrayHelper;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6696")
public class OverrideCustomRevListenerTest extends GloballyConfiguredRevListenerTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return ArrayHelper.join( super.getAnnotatedClasses(), ListenerRevEntity.class );
	}
}
