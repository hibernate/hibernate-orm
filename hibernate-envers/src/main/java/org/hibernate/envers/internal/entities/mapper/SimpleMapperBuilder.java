/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

import org.hibernate.envers.internal.entities.PropertyData;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface SimpleMapperBuilder {
	void add(PropertyData propertyData);
}
