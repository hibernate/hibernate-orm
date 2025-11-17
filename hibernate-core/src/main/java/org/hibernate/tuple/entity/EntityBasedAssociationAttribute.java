/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple.entity;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.AssociationType;

/**
 * @deprecated No direct replacement
 */
@Deprecated(forRemoval = true)
public class EntityBasedAssociationAttribute
		extends AbstractEntityBasedAttribute {


	public EntityBasedAssociationAttribute(
			EntityPersister source,
			SessionFactoryImplementor sessionFactory,
			int attributeNumber,
			String attributeName,
			AssociationType attributeType,
			BaselineAttributeInformation baselineInfo) {
		super( source, sessionFactory, attributeNumber, attributeName, attributeType, baselineInfo );
	}

	@Override
	public AssociationType getType() {
		return (AssociationType) super.getType();
	}

	@Override
	protected String loggableMetadata() {
		return super.loggableMetadata() + ",association";
	}
}
