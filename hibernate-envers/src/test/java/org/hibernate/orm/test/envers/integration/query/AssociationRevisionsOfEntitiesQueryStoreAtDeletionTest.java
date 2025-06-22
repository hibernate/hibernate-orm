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
@JiraKey( value = "HHH-13817" )
public class AssociationRevisionsOfEntitiesQueryStoreAtDeletionTest extends AssociationRevisionsOfEntitiesQueryTest {
	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );
		settings.put( EnversSettings.STORE_DATA_AT_DELETE, true );
	}
}
