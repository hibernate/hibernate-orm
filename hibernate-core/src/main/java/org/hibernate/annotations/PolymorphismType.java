/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.util.Locale;

/**
 * Specifies whether implicit polymorphism is enabled or disabled.
 *
 * @see Polymorphism
 *
 * @author Emmanuel Bernard
 *
 * @deprecated since {@link Polymorphism} is deprecated
 */
@Deprecated(since = "6.2")
public enum PolymorphismType {
	/**
	 * Implicit polymorphism is enabled, and queries against mapped
	 * superclasses and other arbitrary Java supertypes of an entity
	 * will return instances of the entity.
	 */
	IMPLICIT,
	/**
	 * Implicit polymorphism is disabled, and queries against mapped
	 * superclasses and other arbitrary Java supertypes of an entity
	 * will not return the entity.
	 */
	EXPLICIT;

	public static PolymorphismType fromExternalValue(Object externalValue) {
		if ( externalValue != null ) {
			if ( externalValue instanceof PolymorphismType ) {
				return (PolymorphismType) externalValue;
			}

			final String externalValueStr = externalValue.toString();
			for ( PolymorphismType checkType : values() ) {
				if ( checkType.name().equalsIgnoreCase( externalValueStr ) ) {
					return checkType;
				}
			}
		}

		return null;
	}

	public String getExternalForm() {
		return name().toLowerCase( Locale.ROOT );
	}
}
