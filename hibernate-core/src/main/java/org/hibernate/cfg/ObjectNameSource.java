/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

/**
 * Source for database object names (identifiers).
 *
 * @author Steve Ebersole
 */
public interface ObjectNameSource {
	/**
	 * Retrieve the name explicitly provided by the user.
	 *
	 * @return The explicit name.
	 */
	public String getExplicitName();

	/**
	 * Retrieve the logical name for this object.  Usually this is the name under which
	 * the "thing" is registered.
	 * 
	 * @return The logical name.
	 */
	public String getLogicalName();
}
