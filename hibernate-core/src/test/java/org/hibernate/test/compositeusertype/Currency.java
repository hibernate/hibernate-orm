/**
 * 
 */
package org.hibernate.test.compositeusertype;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class Currency implements Unit {

	private final String name;

	public Currency(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		if ( obj instanceof Currency ) {
			result = getName().equals( ( (Currency) obj ).getName() );
		}
		return result;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

}
