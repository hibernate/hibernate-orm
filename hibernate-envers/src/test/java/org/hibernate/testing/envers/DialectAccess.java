/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
