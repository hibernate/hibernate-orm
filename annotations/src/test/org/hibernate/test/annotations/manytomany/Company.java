//$Id$
package org.hibernate.test.annotations.manytomany;

import java.io.Serializable;
import javax.persistence.MappedSuperclass;
import javax.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Company implements Serializable {
	@Column(unique = true) private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
