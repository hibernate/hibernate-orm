/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.revisioninfo;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface RevisionInfoGenerator {
	/**
	 * Set the revision entity number reader instance.
	 */
	void setRevisionInfoNumberReader(RevisionInfoNumberReader revisionInfoNumberReader);

	void saveRevisionData(SharedSessionContractImplementor session, Object revisionData);

	Object generate();

	/**
	 * @see org.hibernate.envers.EntityTrackingRevisionListener#entityChanged(Class, String, Object, RevisionType, Object)
	 */
	void entityChanged(
			Class entityClass, String entityName, Object entityId, RevisionType revisionType,
			Object revisionEntity);
}
