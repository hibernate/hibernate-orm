/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post.fixture;

import org.hibernate.dialect.Dialect;

/// Core call-site fixture for Dialect surface inventory.
///
/// @author Steve Ebersole
public class DialectCaller {
	public String call(Dialect dialect, String value) {
		return dialect.translate( value );
	}
}
