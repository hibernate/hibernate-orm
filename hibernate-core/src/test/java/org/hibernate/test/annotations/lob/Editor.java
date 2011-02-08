//$Id$
package org.hibernate.test.annotations.lob;
import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class Editor implements Serializable {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
