/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetomany.relatedid;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class Employee {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;
	@ManyToOne
	private Company company;

	Employee() {

	}

	public Employee(String name, Company company) {
		this( null, name, company );
	}

	public Employee(Integer id, String name, Company company) {
		this.id = id;
		this.name = name;
		this.company = company;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	@Override
	public int hashCode() {
		int result;
		result = ( id != null ? id.hashCode() : 0 );
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		result = 31 * result + ( company != null ? company.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if ( object == this ) {
			return true;
		}
		if ( !( object instanceof Employee) ) {
			return false;
		}
		Employee that = (Employee) object;
		if ( getId() != null ? !getId().equals( that.getId() ) : that.getId() != null ) {
			return false;
		}
		if ( getName() != null ? !getName().equals( that.getName() ) : that.getName() != null ) {
			return false;
		}
		if ( getCompany() != null ? !getCompany().equals( that.getCompany() ) : that.getCompany() != null ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Employee{" +
				"id=" + id +
				", name='" + name + '\'' +
				", company=" + company +
				'}';
	}
}
