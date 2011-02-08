//$
package org.hibernate.test.annotations.collectionelement.indexedCollection;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Contact {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
