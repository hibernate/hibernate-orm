/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.trackmodifiedentities;

import java.util.Map;

import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.AnnotatedTrackingRevisionEntity;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Tests proper behavior of revision entity that utilizes {@link ModifiedEntityNames} annotation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AnnotatedTrackingEntitiesTest extends DefaultTrackingEntitiesTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return ArrayHelper.join( super.getAnnotatedClasses(), AnnotatedTrackingRevisionEntity.class );
	}

	@Override
	public void addConfigOptions(Map configuration) {
		super.addConfigOptions( configuration );
		configuration.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "false" );
	}
}
