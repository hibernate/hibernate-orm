/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-8058" )
public class EntityWithChangesQueryStoreDeletionDataTest extends AbstractEntityWithChangesQueryTest {
	@Override
	protected void addConfigOptions(Map options) {
		options.put( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, Boolean.TRUE );
		options.put( EnversSettings.STORE_DATA_AT_DELETE, Boolean.TRUE );
		super.addConfigOptions( options );
	}
}
