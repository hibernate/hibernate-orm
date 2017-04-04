/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;

import java.util.Locale;
import org.jboss.logging.Logger;

/**
 * Defines the style that should be used to perform batch loading.  Which style to use is declared using
 * the "{@value org.hibernate.cfg.AvailableSettings#BATCH_FETCH_STYLE}"
 * ({@link org.hibernate.cfg.AvailableSettings#BATCH_FETCH_STYLE}) setting
 *
 * @author Steve Ebersole
 */
public enum BatchFetchStyle {
	/**
	 * The legacy algorithm where we keep a set of pre-built batch sizes based on
	 * {@link org.hibernate.internal.util.collections.ArrayHelper#getBatchSizes}.  Batches are performed
	 * using the next-smaller pre-built batch size from the number of existing batchable identifiers.
	 * <p/>
	 * For example, with a batch-size setting of 32 the pre-built batch sizes would be [32, 16, 10, 9, 8, 7, .., 1].
	 * An attempt to batch load 31 identifiers would result in batches of 16, 10, and 5.
	 */
	LEGACY,
	/**
	 * Still keeps the concept of pre-built batch sizes, but uses the next-bigger batch size and pads the extra
	 * identifier placeholders.
	 * <p/>
	 * Using the same example of a batch-size setting of 32 the pre-built batch sizes would be the same.  However, the
	 * attempt to batch load 31 identifiers would result just a single batch of size 32.  The identifiers to load would
	 * be "padded" (aka, repeated) to make up the difference.
	 */
	PADDED,
	/**
	 * Dynamically builds its SQL based on the actual number of available ids.  Does still limit to the batch-size
	 * defined on the entity/collection
	 */
	DYNAMIC;

	private static final Logger log = Logger.getLogger( BatchFetchStyle.class );

	public static BatchFetchStyle byName(String name) {
		return valueOf( name.toUpperCase(Locale.ROOT) );
	}

	public static BatchFetchStyle interpret(Object setting) {
		log.tracef( "Interpreting BatchFetchStyle from setting : %s", setting );

		if ( setting == null ) {
			return LEGACY; // as default
		}

		if ( BatchFetchStyle.class.isInstance( setting ) ) {
			return (BatchFetchStyle) setting;
		}

		try {
			final BatchFetchStyle byName = byName( setting.toString() );
			if ( byName != null ) {
				return byName;
			}
		}
		catch (Exception ignore) {
		}

		log.debugf( "Unable to interpret given setting [%s] as BatchFetchStyle", setting );

		return LEGACY; // again as default.
	}
}
