package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import java.io.Serializable;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionType;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CustomTrackingRevisionListener implements EntityTrackingRevisionListener {
	@Override
	public void entityChanged(
			Class entityClass, String entityName, Serializable entityId, RevisionType revisionType,
			Object revisionEntity) {
		((CustomTrackingRevisionEntity) revisionEntity).addModifiedEntityType( entityClass.getName() );
	}

	@Override
	public void newRevision(Object revisionEntity) {
	}
}
