package org.hibernate.community.dialect;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

/**
 * The DialectResolver implementation for community maintained dialects
 *
 * @author Christian Beikov
 */
public class CommunityDialectResolver implements DialectResolver {

	@Override
	@Nullable
	public Dialect resolveDialect(@Nonnull DialectResolutionInfo info) {
		for ( CommunityDatabase database : CommunityDatabase.values() ) {
			if ( database.matchesResolutionInfo( info ) ) {
				return database.createDialect( info );
			}
		}

		return null;
	}

}
