/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers;

import org.hibernate.dialect.Dialect;


/**
 * Contract for things that expose a Dialect
 *
 * @author Steve Ebersole
 */
public interface DialectAccess {
	Dialect getDialect();
}
