/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.mapping.Property;

/**
 * @author Steve Ebersole
 */
public record PropertyInfo(Property bootModelProperty) {
	public String tableName() {
		return bootModelProperty.getValue().getTable().getName();
	}
}
