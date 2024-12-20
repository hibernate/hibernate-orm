/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface ExtendedPropertyMapper extends PropertyMapper, CompositeMapperBuilder {
	boolean map(
			SessionImplementor session,
			Map<String, Object> data,
			String[] propertyNames,
			Object[] newState,
			Object[] oldState);
}
