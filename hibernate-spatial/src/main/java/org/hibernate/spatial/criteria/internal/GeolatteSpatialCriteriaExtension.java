/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.criteria.internal;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.spi.CriteriaBuilderExtension;
import org.hibernate.spatial.criteria.GeolatteSpatialCriteriaBuilder;

/**
 * @author Marco Belladelli
 */
public class GeolatteSpatialCriteriaExtension implements CriteriaBuilderExtension {

	@Override
	public HibernateCriteriaBuilder extend(HibernateCriteriaBuilder cb) {
		return new GeolatteSpatialCriteriaBuilderImpl( cb );
	}

	@Override
	public Class<? extends HibernateCriteriaBuilder> getRegistrationKey() {
		return GeolatteSpatialCriteriaBuilder.class;
	}
}
