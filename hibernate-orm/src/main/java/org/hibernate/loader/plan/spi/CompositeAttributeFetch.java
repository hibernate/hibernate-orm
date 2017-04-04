/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

/**
 * Models the requested fetching of a composite attribute.
 *
 * @author Gail Badner
 */
public interface CompositeAttributeFetch extends CompositeFetch, AttributeFetch {
}
