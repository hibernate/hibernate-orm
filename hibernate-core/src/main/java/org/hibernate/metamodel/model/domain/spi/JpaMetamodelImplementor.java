/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;

/**
 * SPI extending {@link JpaMetamodel}.
 *
 * @author Steve Ebersole
 */
public interface JpaMetamodelImplementor extends JpaMetamodel {
	MappingMetamodel getMappingMetamodel();
}
