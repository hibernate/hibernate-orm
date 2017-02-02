/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;


/**
 * Formatter contract
 *
 * @author Steve Ebersole
 */
public interface Formatter {
	/**
	 * Format the source SQL string.
	 *
	 * @param source The original SQL string
	 *
	 * @return The formatted version
	 */
	public String format(String source);
}
