/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing;

import java.util.Locale;

/**
 * Template for native SQL statement
 * <p>
 * This is mainly introduced to avoid a confusion between template and non-template Strings.
 */
public class NativeSQLTemplate {

	final private String nativeSql;

	public NativeSQLTemplate(String template) {
		this.nativeSql = template;
	}

	public String mkNativeSQLString(Object... params) {
		return String.format( Locale.ROOT, nativeSql, params );
	}
}
