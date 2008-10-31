//$Id: $
package org.hibernate.ejb.packaging;

import java.io.InputStream;

/**
 * @author Emmanuel Bernard
 */
public class NamedInputStream {
	public NamedInputStream(String name, InputStream stream) {
		this.name = name;
		this.stream = stream;
	}

	private String name;
	private InputStream stream;

	public InputStream getStream() {
		return stream;
	}

	public void setStream(InputStream stream) {
		this.stream = stream;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
