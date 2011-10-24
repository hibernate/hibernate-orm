//$Id$
package org.hibernate.test.annotations.collectionelement;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.MapKeyColumn;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class LocalizedString implements Serializable {

	private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

	public LocalizedString() {
	}

	public LocalizedString(String string) {
		this.getVariations().put( DEFAULT_LOCALE.getLanguage(), string );
	}

	private Map<String, String> variations =
			new HashMap<String, String>( 1 );

	@ElementCollection
	@MapKeyColumn(name = "language_code" )
	@Fetch( FetchMode.JOIN )
	@Filter( name = "selectedLocale",
			condition = " language_code = :param " )
	public Map<String, String> getVariations() {
		return variations;
	}

	public void setVariations(Map<String, String> variations) {
		this.variations = variations;
	}
}
