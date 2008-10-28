//$Id$
package org.hibernate.test.annotations.target;

import java.util.Map;
import java.util.HashMap;
import javax.persistence.Embeddable;
import org.hibernate.annotations.MapKey;
import javax.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class OwnerImpl implements Owner {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
