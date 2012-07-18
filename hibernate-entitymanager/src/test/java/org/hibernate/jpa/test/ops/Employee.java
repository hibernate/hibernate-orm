//$Id$
package org.hibernate.jpa.test.ops;
import java.io.Serializable;
import java.util.Collection;


/**
 * Employee in an Employer-Employee relationship
 *
 * @author Emmanuel Bernard
 */

public class Employee implements Serializable {
	private Integer id;
	private Collection employers;


	public Integer getId() {
		return id;
	}

	public void setId(Integer integer) {
		id = integer;
	}


	public Collection getEmployers() {
		return employers;
	}

	public void setEmployers(Collection employers) {
		this.employers = employers;
	}
}
