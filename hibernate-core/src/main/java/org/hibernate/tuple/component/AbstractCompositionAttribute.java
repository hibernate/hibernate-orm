/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple.component;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.tuple.AbstractNonIdentifierAttribute;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.CompositeType;

/**
 * @deprecated No direct replacement
 */
@Deprecated(forRemoval = true)
public abstract class AbstractCompositionAttribute
		extends AbstractNonIdentifierAttribute {

	protected AbstractCompositionAttribute(
			AttributeSource source,
			SessionFactoryImplementor sessionFactory,
			int entityBasedAttributeNumber,
			String attributeName,
			CompositeType attributeType,
			int columnStartPosition,
			BaselineAttributeInformation baselineInfo) {
		super( source, sessionFactory, entityBasedAttributeNumber, attributeName, attributeType, baselineInfo );
	}

	@Override
	public CompositeType getType() {
		return (CompositeType) super.getType();
	}

	protected abstract EntityPersister locateOwningPersister();

	@Override
	protected String loggableMetadata() {
		return super.loggableMetadata() + ",composition";
	}
}
