/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import java.io.Serializable;

/**
 * Contract to allow inspection (and swapping) of SQL to be prepared
 *
 * @author Steve Ebersole
 */
public interface StatementInspector extends Serializable {
	/**
	 * Inspect the given SQL, possibly returning a different SQL to be used instead.  Note that returning {@code null}
	 * is interpreted as returning the same SQL as was passed.
	 *
	 * @param sql The SQL to inspect
	 *
	 * @return The SQL to use; may be {@code null}
	 */
	public String inspect(String sql);
}
