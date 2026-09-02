/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lob.internal;

import java.sql.DatabaseMetaData;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.lob.spi.LobSupport;

import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;

/// Built-in immutable LOB policy profiles.
///
/// @author Steve Ebersole
/// @since 8.0
public final class StandardLobSupport implements LobSupport {
	private enum Kind { STANDARD, NO_PROMOTION, NO_CONTEXTUAL, NON_STREAMING, POSTGRESQL, ORACLE }

	private static final LobSupport STANDARD = new StandardLobSupport( Kind.STANDARD, false, false );
	private static final LobSupport NO_PROMOTION = new StandardLobSupport( Kind.NO_PROMOTION, false, false );
	private static final LobSupport NO_CONTEXTUAL = new StandardLobSupport( Kind.NO_CONTEXTUAL, false, false );
	private static final LobSupport NON_STREAMING = new StandardLobSupport( Kind.NON_STREAMING, false, false );
	private static final LobSupport POSTGRESQL = new StandardLobSupport( Kind.POSTGRESQL, false, false );
	private static final LobSupport[] ORACLE = {
			new StandardLobSupport( Kind.ORACLE, false, false ),
			new StandardLobSupport( Kind.ORACLE, false, true ),
			new StandardLobSupport( Kind.ORACLE, true, false ),
			new StandardLobSupport( Kind.ORACLE, true, true )
	};

	private final Kind kind;
	private final boolean applicationContinuity;
	private final boolean valueLobAccess;

	private StandardLobSupport(Kind kind, boolean applicationContinuity, boolean valueLobAccess) {
		this.kind = kind;
		this.applicationContinuity = applicationContinuity;
		this.valueLobAccess = valueLobAccess;
	}

	public static LobSupport standard() {
		return STANDARD;
	}

	public static LobSupport noCapacityPromotion() {
		return NO_PROMOTION;
	}

	public static LobSupport noContextualCreation() {
		return NO_CONTEXTUAL;
	}

	public static LobSupport nonStreaming() {
		return NON_STREAMING;
	}

	public static LobSupport postgresql() {
		return POSTGRESQL;
	}

	public static LobSupport oracle(boolean applicationContinuity, boolean valueLobAccess) {
		return ORACLE[(applicationContinuity ? 2 : 0) + (valueLobAccess ? 1 : 0)];
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(@Nullable DatabaseMetaData databaseMetaData) {
		return kind != Kind.NO_CONTEXTUAL && kind != Kind.POSTGRESQL;
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return switch ( kind ) {
			case NON_STREAMING, POSTGRESQL -> false;
			case ORACLE -> !applicationContinuity;
			default -> true;
		};
	}

	@Override
	public boolean useConnectionToCreateLob() {
		return switch ( kind ) {
			case NON_STREAMING, POSTGRESQL -> false;
			default -> LobSupport.super.useConnectionToCreateLob();
		};
	}

	@Override
	public boolean supportsMaterializedLobAccess() {
		return kind != Kind.POSTGRESQL;
	}

	@Override
	public boolean useMaterializedLobWhenCapacityExceeded() {
		return kind != Kind.NO_PROMOTION && LobSupport.super.useMaterializedLobWhenCapacityExceeded();
	}

	@Override
	public boolean forceLobAsLastValue() {
		return kind == Kind.ORACLE;
	}

	@Override
	public boolean isLobType(int sqlTypeCode) {
		if ( kind == Kind.POSTGRESQL ) {
			return switch ( sqlTypeCode ) {
				case LONG32VARCHAR, LONG32NVARCHAR, LONG32VARBINARY -> false;
				default -> LobSupport.super.isLobType( sqlTypeCode );
			};
		}
		return LobSupport.super.isLobType( sqlTypeCode );
	}

	@Override
	public @Nullable String getValueLobFragmentForExtraCreateTableInfo(String columnName) {
		if ( columnName == null ) {
			throw new IllegalArgumentException( "Column name must not be null" );
		}
		return kind == Kind.ORACLE && valueLobAccess
				? " lob(" + columnName + ") query as value"
				: null;
	}
}
