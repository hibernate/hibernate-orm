/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader;

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
		return valueOf( name.toUpperCase() );
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
