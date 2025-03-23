/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionType;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CustomTrackingRevisionListener implements EntityTrackingRevisionListener {
	@Override
	public void entityChanged(
			Class entityClass, String entityName, Object entityId, RevisionType revisionType,
			Object revisionEntity) {
		((CustomTrackingRevisionEntity) revisionEntity).addModifiedEntityType( entityClass.getName() );
	}

	@Override
	public void newRevision(Object revisionEntity) {
	}
}
