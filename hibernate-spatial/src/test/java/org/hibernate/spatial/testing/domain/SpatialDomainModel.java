/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.domain;

import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;

public class SpatialDomainModel extends AbstractDomainModelDescriptor {
	public SpatialDomainModel() {
		super(
				GeomEntity.class,
				JtsGeomEntity.class
		);
	}
}
