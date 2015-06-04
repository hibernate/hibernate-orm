/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

/**
 * Describes access to a particular persistent property in terms of getting and setting
 * values.
 * <p/>
 * Instances are obtained from {@link PropertyAccessStrategy}
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface PropertyAccess {
	/**
	 * Access to the PropertyAccessStrategy that created this PropertyAccess
	 *
	 * @return The PropertyAccessStrategy that created this PropertyAccess
	 */
	PropertyAccessStrategy getPropertyAccessStrategy();

	/**
	 * Obtain the delegate for getting values for the described persistent property
	 *
	 * @return The property getter
	 */
	Getter getGetter();

	/**
	 * Obtain the delegate for setting values for the described persistent property
	 *
	 * @return The property setter
	 */
	Setter getSetter();
}
