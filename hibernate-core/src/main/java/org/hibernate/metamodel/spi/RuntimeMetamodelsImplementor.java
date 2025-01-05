/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.type.MappingContext;

/**
 * SPI extending {@link RuntimeMetamodels} and mixing in {@link MappingContext}.
 *
 * @author Steve Ebersole
 */
public interface RuntimeMetamodelsImplementor extends RuntimeMetamodels, MappingContext {
	@Override
	MappingMetamodelImplementor getMappingMetamodel();

	@Override
	JpaMetamodelImplementor getJpaMetamodel();
}
