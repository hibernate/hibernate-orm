/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing;

import java.util.Locale;

public class HQLTemplate {

	final private String hqlTemplate;

	public HQLTemplate(String template) {
		this.hqlTemplate = template;
	}

	public static HQLTemplate from(String s) {
		return new HQLTemplate( s );
	}

	public String mkHQLString(Object... params) {
		return String.format( Locale.ROOT, hqlTemplate, params );
	}
}
