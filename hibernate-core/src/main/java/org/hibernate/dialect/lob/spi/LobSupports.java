/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lob.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.lob.internal.StandardLobSupport;

import static org.hibernate.SPI.Role.USE;

/// Supplies immutable stock LOB policy profiles.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class LobSupports {
	private LobSupports() {
	}

	/// Return the standard LOB policy.
	public static LobSupport standard() {
		return StandardLobSupport.standard();
	}

	/// Return the standard policy without capacity-driven LOB promotion.
	public static LobSupport noCapacityPromotion() {
		return StandardLobSupport.noCapacityPromotion();
	}

	/// Return the standard policy without contextual JDBC LOB creation.
	public static LobSupport noContextualCreation() {
		return StandardLobSupport.noContextualCreation();
	}

	/// Return the non-streaming policy which also avoids forcing Connection
	/// factories.
	public static LobSupport nonStreaming() {
		return StandardLobSupport.nonStreaming();
	}

	/// Return the PostgreSQL LOB policy.
	public static LobSupport postgresql() {
		return StandardLobSupport.postgresql();
	}

	/// Return the Oracle policy selected by immutable server configuration.
	public static LobSupport oracle(boolean applicationContinuity, boolean valueLobAccess) {
		return StandardLobSupport.oracle( applicationContinuity, valueLobAccess );
	}
}
