/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;

import org.hibernate.PropertyNotFoundException;

/**
 * @author max
 */
public class ChainedPropertyAccessor implements PropertyAccessor {

	final PropertyAccessor[] chain;

	public ChainedPropertyAccessor(PropertyAccessor[] chain) {
		this.chain = chain;
	}

	public Getter getGetter(Class theClass, String propertyName)
			throws PropertyNotFoundException {
		Getter result = null;
		for ( PropertyAccessor candidate : chain ) {
			try {
				result = candidate.getGetter( theClass, propertyName );
				return result;
			}
			catch (PropertyNotFoundException pnfe) {
				// ignore
			}
		}
		throw new PropertyNotFoundException( "Could not find getter for " + propertyName + " on " + theClass );
	}

	public Setter getSetter(Class theClass, String propertyName)
			throws PropertyNotFoundException {
		Setter result = null;
		for ( PropertyAccessor candidate : chain ) {
			try {
				result = candidate.getSetter( theClass, propertyName );
				return result;
			}
			catch (PropertyNotFoundException pnfe) {
				// ignore
			}
		}
		throw new PropertyNotFoundException( "Could not find setter for " + propertyName + " on " + theClass );
	}

}
