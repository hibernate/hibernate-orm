/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.persister.walking.spi.AttributeDefinition;

/**
 * Models a {@link Fetch} that is specifically for an attribute.
 *
 * @author Gail Badner
 */
public interface AttributeFetch extends Fetch {

	/**
	 * Returns the {@link AttributeDefinition} for attribute being fetched.
	 *
	 * @return The fetched attribute definition.
	 */
	public AttributeDefinition getFetchedAttributeDefinition();
}
