//$
package org.hibernate.ejb.packaging;

import java.io.InputStream;

/**
 * Represent a JAR entry
 * Contains a name and an optional Input stream to the entry
 *
 * @author Emmanuel Bernard
 */
public class Entry {
	private String name;
	private InputStream is;

	public Entry(String name, InputStream is) {
		this.name = name;
		this.is = is;
	}

	public String getName() {
		return name;
	}

	public InputStream getInputStream() {
		return is;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final Entry entry = (Entry) o;

		if ( !name.equals( entry.name ) ) return false;

		return true;
	}

	public int hashCode() {
		return name.hashCode();
	}
}