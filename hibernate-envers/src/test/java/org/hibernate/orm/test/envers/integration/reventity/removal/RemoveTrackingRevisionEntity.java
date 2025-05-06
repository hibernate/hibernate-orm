/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.removal;

import java.util.Map;

import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialectFeature(DialectChecks.SupportsCascadeDeleteCheck.class)
public class RemoveTrackingRevisionEntity extends AbstractRevisionEntityRemovalTest {
	@Override
	public void addConfigOptions(Map configuration) {
		super.addConfigOptions( configuration );
		configuration.put("org.hibernate.envers.track_entities_changed_in_revision", "true");
	}

	@Override
	protected Class<?> getRevisionEntityClass() {
		return SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
	}
}
