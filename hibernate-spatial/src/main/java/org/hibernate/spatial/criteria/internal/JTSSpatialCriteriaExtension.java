/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.criteria.internal;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.spi.CriteriaBuilderExtension;
import org.hibernate.spatial.criteria.JTSSpatialCriteriaBuilder;

/**
 * @author Marco Belladelli
 */
public class JTSSpatialCriteriaExtension implements CriteriaBuilderExtension {

	@Override
	public HibernateCriteriaBuilder extend(HibernateCriteriaBuilder cb) {
		return new JTSSpatialCriteriaBuilderImpl( cb );
	}

	@Override
	public Class<? extends HibernateCriteriaBuilder> getRegistrationKey() {
		return JTSSpatialCriteriaBuilder.class;
	}
}
