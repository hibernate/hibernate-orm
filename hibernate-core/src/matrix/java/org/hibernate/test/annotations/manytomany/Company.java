//$Id$
package org.hibernate.test.annotations.manytomany;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Company implements Serializable {
	@Column
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
