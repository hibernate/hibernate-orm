/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.complete;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.results.ResultBuilderEntityValued;

/**
 * @author Steve Ebersole
 */
public interface CompleteResultBuilderEntityValued
		extends CompleteResultBuilder, ModelPartReferenceEntity, ResultBuilderEntityValued {
	@Override
	EntityMappingType getReferencedPart();
}
