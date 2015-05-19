/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.mapping.Property;

/**
 * @author Steve Ebersole
 */
public interface NaturalIdUniqueKeyBinder {
	/**
	 * Adds a attribute binding.  The attribute is a (top-level) part of the natural-id
	 *
	 * @param attributeBinding The attribute binding that is part of the natural-id
	 */
	public void addAttributeBinding(Property attributeBinding);

	public void process();
}
