/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identifier.spi;

import org.hibernate.SPI;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Supported base for selectively decorating a configured identifier helper.
///
/// First configure the builder received by
/// [IdentifierSupport#buildIdentifierHelper(IdentifierHelperBuildRequest)] and
/// invoke the inherited build operation. Extend this class only when the
/// resulting helper requires a database-specific rule which builder
/// configuration cannot express. Override every related entry point which must
/// apply that rule; each inherited operation forwards independently.
///
/// Call `super` to preserve ordinary forwarding or use [#delegate()] when an
/// override must invoke a different delegate operation. Retaining the delegate
/// as part of the returned runtime helper is supported. Do not retain the
/// boot-scoped build request, builder, or metadata view.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see IdentifierHelper
/// @see IdentifierSupport#buildIdentifierHelper(IdentifierHelperBuildRequest)
@SPI({ USE, IMPLEMENT })
public abstract class DelegatingIdentifierHelper implements IdentifierHelper {
	private final IdentifierHelper delegate;

	/// Create a decorator around the non-null configured helper.
	///
	/// @since 8.0
	@SPI(IMPLEMENT)
	protected DelegatingIdentifierHelper(IdentifierHelper delegate) {
		this.delegate = requireNonNull( delegate, "delegate" );
	}

	/// Access the stable configured helper owned by this decorator.
	///
	/// @since 8.0
	@SPI(USE)
	protected final IdentifierHelper delegate() {
		return delegate;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public Identifier normalizeQuoting(Identifier identifier) {
		return delegate.normalizeQuoting( identifier );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public Identifier toIdentifier(String text) {
		return delegate.toIdentifier( text );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public Identifier toIdentifier(String text, boolean quoted) {
		return delegate.toIdentifier( text, quoted );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public Identifier toIdentifier(String text, boolean quoted, boolean isExplicit) {
		return delegate.toIdentifier( text, quoted, isExplicit );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public Identifier applyGlobalQuoting(String text) {
		return delegate.applyGlobalQuoting( text );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean isReservedWord(String word) {
		return delegate.isReservedWord( word );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String toMetaDataCatalogName(Identifier catalogIdentifier) {
		return delegate.toMetaDataCatalogName( catalogIdentifier );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String toMetaDataSchemaName(Identifier schemaIdentifier) {
		return delegate.toMetaDataSchemaName( schemaIdentifier );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String toMetaDataObjectName(Identifier identifier) {
		return delegate.toMetaDataObjectName( identifier );
	}
}
