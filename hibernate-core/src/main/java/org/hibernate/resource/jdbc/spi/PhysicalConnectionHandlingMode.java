/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.spi;

import java.util.Locale;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;

import static java.util.Objects.requireNonNull;
import static org.hibernate.ConnectionAcquisitionMode.AS_NEEDED;
import static org.hibernate.ConnectionAcquisitionMode.IMMEDIATELY;
import static org.hibernate.ConnectionReleaseMode.AFTER_STATEMENT;
import static org.hibernate.ConnectionReleaseMode.AFTER_TRANSACTION;
import static org.hibernate.ConnectionReleaseMode.BEFORE_TRANSACTION_COMPLETION;
import static org.hibernate.ConnectionReleaseMode.ON_CLOSE;
import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * Enumerates valid combinations of {@link ConnectionAcquisitionMode} and
 * {@link ConnectionReleaseMode}.
 *
 * @see org.hibernate.cfg.AvailableSettings#CONNECTION_HANDLING
 *
 * @author Steve Ebersole
 */
public enum PhysicalConnectionHandlingMode {
	/**
	 * The {@code Connection} will be acquired as soon as the session is opened
	 * and held until the session is closed.  This is the only valid combination
	 * including immediate acquisition of the Connection
	 */
	IMMEDIATE_ACQUISITION_AND_HOLD( IMMEDIATELY, ON_CLOSE ),
	/**
	 * The {@code Connection} will be acquired as soon as it is needed and then
	 * held until the session is closed.  This is the original Hibernate behavior.
	 */
	DELAYED_ACQUISITION_AND_HOLD( AS_NEEDED, ON_CLOSE ),
	/**
	 * The {@code Connection} will be acquired as soon as it is needed; it will be
	 * released after each statement is executed.
	 */
	DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT( AS_NEEDED, AFTER_STATEMENT ),
	/**
	 * The {@code Connection} will be acquired as soon as it is needed; it will be
	 * released before commit or rollback.
	 */
	DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION( AS_NEEDED, BEFORE_TRANSACTION_COMPLETION ),
	/**
	 * The {@code Connection} will be acquired as soon as it is needed; it will be
	 * released after each transaction is completed.
	 */
	DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION( AS_NEEDED, AFTER_TRANSACTION )
	;

	private final ConnectionAcquisitionMode acquisitionMode;
	private final ConnectionReleaseMode releaseMode;

	PhysicalConnectionHandlingMode(
			ConnectionAcquisitionMode acquisitionMode,
			ConnectionReleaseMode releaseMode) {
		this.acquisitionMode = acquisitionMode;
		this.releaseMode = releaseMode;
	}

	public ConnectionAcquisitionMode getAcquisitionMode() {
		return acquisitionMode;
	}

	public ConnectionReleaseMode getReleaseMode() {
		return releaseMode;
	}

	public static PhysicalConnectionHandlingMode interpret(Object setting) {
		if ( setting instanceof PhysicalConnectionHandlingMode mode ) {
			return mode;
		}
		else if ( setting instanceof String string ) {
			return isBlank( string ) ? null
					: valueOf( string.trim().toUpperCase( Locale.ROOT ) );

		}
		else {
			return null;
		}
	}

	public static PhysicalConnectionHandlingMode interpret(
			ConnectionAcquisitionMode acquisitionMode,
			ConnectionReleaseMode releaseMode) {
		requireNonNull( acquisitionMode, "ConnectionAcquisitionMode must be specified" );
		requireNonNull( acquisitionMode, "ConnectionReleaseMode must be specified" );
		return switch ( acquisitionMode ) {
			case AS_NEEDED -> switch ( releaseMode ) {
				case ON_CLOSE -> DELAYED_ACQUISITION_AND_HOLD;
				case AFTER_STATEMENT -> DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT;
				case BEFORE_TRANSACTION_COMPLETION -> DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION;
				case AFTER_TRANSACTION -> DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
			};
			case IMMEDIATELY -> switch ( releaseMode ) {
				case ON_CLOSE -> IMMEDIATE_ACQUISITION_AND_HOLD;
				default -> throw new IllegalArgumentException(
						"Only ConnectionReleaseMode.ON_CLOSE can be used in combination with "
						+ "ConnectionAcquisitionMode.IMMEDIATELY; but ConnectionReleaseMode."
						+ releaseMode.name() + " was specified."
				);
			};
		};
	}
}
