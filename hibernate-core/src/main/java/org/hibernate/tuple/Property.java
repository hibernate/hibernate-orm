/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

/**
 * Defines the basic contract of a Property within the runtime metamodel.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use the direct {@link Attribute} hierarchy
 */
@Deprecated
public interface Property extends Attribute {
	/**
	 * @deprecated DOM4j entity mode is no longer supported
	 */
	@Deprecated
	public String getNode();
}
