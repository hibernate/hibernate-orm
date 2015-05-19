/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Maps a given discriminator value to the corresponding entity type.  See {@link Any} for more information.
 *
 * @see Any
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public @interface MetaValue {
	/**
	 * The entity type.
	 */
	Class targetEntity();

	/**
	 * The corresponding discriminator value stored in database.
	 */
	String value();
}
