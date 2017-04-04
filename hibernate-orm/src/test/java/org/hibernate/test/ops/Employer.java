/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Employer.java 8670 2005-11-25 17:36:29Z epbernard $
package org.hibernate.test.ops;
import java.io.Serializable;
import java.util.Collection;


/**
 * Employer in a employer-Employee relationship
 * 
 * @author Emmanuel Bernard
 */

public class Employer implements Serializable {
	private Integer id;
	private Collection employees;
	private Integer vers;

	public Integer getVers() {
		return vers;
	}

	public void setVers(Integer vers) {
		this.vers = vers;
	}


	public Collection getEmployees() {
		return employees;
	}
	
	
	public Integer getId() {
		return id;
	}
	
	public void setEmployees(Collection set) {
		employees = set;
	}

	public void setId(Integer integer) {
		id = integer;
	}
}
