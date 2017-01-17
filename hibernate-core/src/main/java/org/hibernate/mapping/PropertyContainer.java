/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

/**
 * Compliment to {@link AttributeContainer}, but unrelated to allow these to
 * grow separately.  Also it is generally well-known whether you need an accessor or a mutator.
 *
 * @author Steve Ebersole
 */
public interface PropertyContainer {
	PropertyContainer getSuperPropertyContainer();

	java.util.List<Property> getDeclaredProperties();
}
