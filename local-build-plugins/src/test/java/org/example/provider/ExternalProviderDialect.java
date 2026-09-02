/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.provider;

import org.hibernate.dialect.SampleDialect;
import org.hibernate.sql.ast.internal.InternalTranslator;

/// Non-Hibernate-package provider fixture used to prove external bytecode
/// discovery.
///
/// @author Steve Ebersole
public class ExternalProviderDialect extends SampleDialect {
	@Override
	public String translate(String value) {
		return new InternalTranslator().translate( value );
	}
}
