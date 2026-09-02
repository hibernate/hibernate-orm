/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.identifier.spi.DelegatingIdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

/// Provider-owned identifier helper which quotes names beginning with
/// `provider_`.
///
/// The configured Hibernate helper retains ownership of every other identifier
/// and metadata rule.
///
/// @author Steve Ebersole
/// @since 8.0
// tag::identifier-helper-decorator[]
public final class ExampleIdentifierHelper extends DelegatingIdentifierHelper {
	public ExampleIdentifierHelper(IdentifierHelper delegate) {
		super( delegate );
	}

	@Override
	public Identifier normalizeQuoting(Identifier identifier) {
		final Identifier normalized = super.normalizeQuoting( identifier );
		return normalized != null
				&& !normalized.isQuoted()
				&& normalized.getText().startsWith( "provider_" )
				? normalized.quoted()
				: normalized;
	}

	@Override
	public Identifier toIdentifier(String text) {
		return normalizeQuoting( Identifier.toIdentifier( text ) );
	}

	@Override
	public Identifier toIdentifier(String text, boolean quoted) {
		return normalizeQuoting( Identifier.toIdentifier( text, quoted ) );
	}

	@Override
	public Identifier toIdentifier(String text, boolean quoted, boolean isExplicit) {
		return normalizeQuoting( Identifier.toIdentifier( text, quoted, false, isExplicit ) );
	}
}
// end::identifier-helper-decorator[]
