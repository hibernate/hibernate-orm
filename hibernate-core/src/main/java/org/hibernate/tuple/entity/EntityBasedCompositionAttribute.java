/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple.entity;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.tuple.component.AbstractCompositionAttribute;
import org.hibernate.type.CompositeType;

/**
 * @deprecated No direct replacement
 */
@Deprecated(forRemoval = true)
public class EntityBasedCompositionAttribute
		extends AbstractCompositionAttribute {

	public EntityBasedCompositionAttribute(
			EntityPersister source,
			SessionFactoryImplementor factory,
			int attributeNumber,
			String attributeName,
			CompositeType attributeType,
			BaselineAttributeInformation baselineInfo) {
		super( source, factory, attributeNumber, attributeName, attributeType, 0, baselineInfo );
	}

	@Override
	protected EntityPersister locateOwningPersister() {
		return (EntityPersister) getSource();
	}
}
