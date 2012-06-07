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
