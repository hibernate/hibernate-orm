//$
package org.hibernate.ejb.packaging;

/**
 * Filter used when searching elements in a JAR
 *
 * @author Emmanuel Bernard
 */
public abstract class Filter {
	private boolean retrieveStream;

	protected Filter(boolean retrieveStream) {
		this.retrieveStream = retrieveStream;
	}

	public boolean getStream() {
		return retrieveStream;
	}
}
