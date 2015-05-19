/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * @author Steve Ebersole
 */
public interface SizeSource {
	/**
	 * The specified length.  Will return {@code null} if none was specified.
	 *
	 * @return The length, or {@code null} if not defined.
	 */
	Integer getLength();

	/**
	 * The specified precision.  Will return {@code null} if none was specified.
	 *
	 * @return The precision, or {@code null} if not defined.
	 */
	Integer getPrecision();

	/**
	 * The specified scale.  Will return {@code null} if none was specified.
	 *
	 * @return The scale, or {@code null} if not defined.
	 */
	Integer getScale();
}
