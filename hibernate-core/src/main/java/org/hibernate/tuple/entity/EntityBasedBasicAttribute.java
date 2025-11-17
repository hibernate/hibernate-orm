/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple.entity;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.Type;

/**
 * @deprecated No direct replacement
 */
@Deprecated(forRemoval = true)
public class EntityBasedBasicAttribute extends AbstractEntityBasedAttribute {
	public EntityBasedBasicAttribute(
			EntityPersister source,
			SessionFactoryImplementor factory,
			int attributeNumber,
			String attributeName,
			Type attributeType,
			BaselineAttributeInformation baselineInfo) {
		super( source, factory, attributeNumber, attributeName, attributeType, baselineInfo );
	}
}
