package org.hibernate.query.sqm.spi;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface SqmParameterMappingModelResolutionAccess {
	<T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter);
}
