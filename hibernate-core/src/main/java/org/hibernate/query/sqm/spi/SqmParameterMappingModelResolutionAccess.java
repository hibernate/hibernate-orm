/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface SqmParameterMappingModelResolutionAccess {
	<T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter);
}
