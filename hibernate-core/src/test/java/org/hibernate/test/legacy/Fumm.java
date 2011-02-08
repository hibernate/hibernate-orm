//$Id: Fumm.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.Locale;

public class Fumm {
	
	private Locale locale;
	private Fum fum;
	
	public FumCompositeID getId() {
		return fum.getId();
	}
	public void setId(FumCompositeID id) {
	}
	
	public Fumm() {
		super();
	}
	
	/**
	 * Returns the fum.
	 * @return Fum
	 */
	public Fum getFum() {
		return fum;
	}
	
	/**
	 * Returns the locale.
	 * @return Locale
	 */
	public Locale getLocale() {
		return locale;
	}
	
	/**
	 * Sets the fum.
	 * @param fum The fum to set
	 */
	public void setFum(Fum fum) {
		this.fum = fum;
	}
	
	/**
	 * Sets the locale.
	 * @param locale The locale to set
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
}






