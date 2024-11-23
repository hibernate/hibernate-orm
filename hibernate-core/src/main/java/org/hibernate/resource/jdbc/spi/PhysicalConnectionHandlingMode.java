/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import java.util.Locale;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.ConnectionAcquisitionMode.AS_NEEDED;
import static org.hibernate.ConnectionAcquisitionMode.IMMEDIATELY;
import static org.hibernate.ConnectionReleaseMode.AFTER_STATEMENT;
import static org.hibernate.ConnectionReleaseMode.AFTER_TRANSACTION;
import static org.hibernate.ConnectionReleaseMode.BEFORE_TRANSACTION_COMPLETION;
import static org.hibernate.ConnectionReleaseMode.ON_CLOSE;

/**
 * Represents valid combinations of ConnectionAcquisitionMode and ConnectionReleaseMode
 *
 * @author Steve Ebersole
 */
public enum PhysicalConnectionHandlingMode {
	/**
	 * The Connection will be acquired as soon as the Session is opened and
	 * held until the Session is closed.  This is the only valid combination
	 * including immediate acquisition of the Connection
	 */
	IMMEDIATE_ACQUISITION_AND_HOLD( IMMEDIATELY, ON_CLOSE ),
	/**
	 * The Connection will be acquired as soon as it is needed and then held
	 * until the Session is closed.  This is the original Hibernate behavior.
	 */
	DELAYED_ACQUISITION_AND_HOLD( AS_NEEDED, ON_CLOSE ),
	/**
	 * The Connection will be acquired as soon as it is needed; it will be released
	 * after each statement is executed.
	 */
	DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT( AS_NEEDED, AFTER_STATEMENT ),
	/**
	 * The Connection will be acquired as soon as it is needed; it will be released
	 * before commit/rollback.
	 */
	DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION( AS_NEEDED, BEFORE_TRANSACTION_COMPLETION ),
	/**
	 * The Connection will be acquired as soon as it is needed; it will be released
	 * after each transaction is completed.
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
		if ( setting == null ) {
			return null;
		}

		if ( setting instanceof PhysicalConnectionHandlingMode ) {
			return (PhysicalConnectionHandlingMode) setting;
		}

		final String value = setting.toString().trim();
		if ( value.isEmpty() ) {
			return null;
		}

		return PhysicalConnectionHandlingMode.valueOf( value.toUpperCase( Locale.ROOT ) );
	}

	public static PhysicalConnectionHandlingMode interpret(
			ConnectionAcquisitionMode acquisitionMode,
			ConnectionReleaseMode releaseMode) {
		if ( acquisitionMode == IMMEDIATELY ) {
			if ( releaseMode != null && releaseMode != ON_CLOSE ) {
				throw new IllegalArgumentException(
						"Only ConnectionReleaseMode.ON_CLOSE can be used in combination with " +
								"ConnectionAcquisitionMode.IMMEDIATELY; but ConnectionReleaseMode." +
								releaseMode.name() + " was specified."
				);
			}
			return IMMEDIATE_ACQUISITION_AND_HOLD;
		}
		else {
			switch ( releaseMode ) {
				case AFTER_STATEMENT: {
					return DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT;
				}
				case BEFORE_TRANSACTION_COMPLETION: {
					return DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION;
				}
				case AFTER_TRANSACTION: {
					return DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
				}
				default: {
					return DELAYED_ACQUISITION_AND_HOLD;
				}
			}
		}
	}
}
