/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;

/**
 * SPI extending {@link JpaMetamodel}.
 *
 * @author Steve Ebersole
 */
public interface JpaMetamodelImplementor extends JpaMetamodel {
	MappingMetamodel getMappingMetamodel();

	@Override
	RootGraphImplementor<?> findEntityGraphByName(String name);
}
