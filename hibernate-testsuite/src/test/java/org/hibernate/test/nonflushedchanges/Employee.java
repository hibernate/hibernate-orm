//$Id: Employee.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.nonflushedchanges;

import java.io.Serializable;
import java.util.Collection;


/**
 * Employee in an Employer-Employee relationship
 *
 * @author Emmanuel Bernard, Gail Badner (adapted this from "ops" tests version)
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
