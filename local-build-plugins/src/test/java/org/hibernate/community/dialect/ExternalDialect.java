/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.SampleDialect;
import org.hibernate.community.dialect.internal.ProviderHelper;
import org.hibernate.sql.ast.internal.InternalTranslator;

/// Community provider fixture with a deliberate dependency on an internal
/// translator.
///
/// @author Steve Ebersole
public class ExternalDialect extends SampleDialect {
	@Override
	public String translate(String value) {
		return new ProviderHelper().apply( new InternalTranslator().translate( value ) );
	}
}
