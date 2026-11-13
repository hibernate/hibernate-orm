package org.hibernate.engine.jdbc.dialect.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

/**
 * The standard {@link DialectResolver} implementation
 *
 * @author Steve Ebersole
 */
public final class StandardDialectResolver implements DialectResolver {
	public StandardDialectResolver() {
	}

	@Override
	@Nullable
	public Dialect resolveDialect(@Nonnull DialectResolutionInfo info) {

		for ( Database database : Database.values() ) {
			if ( database.matchesResolutionInfo( info ) ) {
				return database.createDialect( info );
			}
		}

		return null;
	}
}
