/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;

/**
 * @author Steve Ebersole
 */
// todo (6.0) : how is this different from jpa.org.hibernate.query.sqm.tree.spi.ParameterCollector?
public interface ParameterCollector {
	void addParameter(SqmParameter<?> parameter);
}
