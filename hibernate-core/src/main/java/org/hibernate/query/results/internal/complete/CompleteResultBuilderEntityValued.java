package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.results.spi.ResultBuilderEntityValued;

/**
 * @author Steve Ebersole
 */
public interface CompleteResultBuilderEntityValued
		extends CompleteResultBuilder, ModelPartReferenceEntity, ResultBuilderEntityValued {
	@Override
	EntityMappingType getReferencedPart();
}
