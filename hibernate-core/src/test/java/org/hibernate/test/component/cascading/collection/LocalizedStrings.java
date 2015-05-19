/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.cascading.collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class LocalizedStrings {
	private Map strings = new HashMap();

	public void addString(Locale locale, String value) {
		strings.put( locale, value );
	}

	public String getString(Locale locale) {
		return ( String ) strings.get( locale );
	}

	public Map getStringsCopy() {
		return java.util.Collections.unmodifiableMap( strings );
	}
}
