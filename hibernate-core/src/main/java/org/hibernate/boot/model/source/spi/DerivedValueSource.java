/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contract describing source of a derived value (formula).
 *
 * @author Steve Ebersole
 */
public interface DerivedValueSource extends RelationalValueSource {
	/**
	 * Obtain the expression used to derive the value.
	 *
	 * @return The derived value expression.
	 */
	String getExpression();
}
