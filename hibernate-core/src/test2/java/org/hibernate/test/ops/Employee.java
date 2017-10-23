/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Employee.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.ops;
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
