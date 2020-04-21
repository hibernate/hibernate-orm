/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.reflection;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity(name = "JavaAdministration")
@Table(name = "JavaAdministration")
@SecondaryTable(name = "Extend")
public class Administration extends Organization {
	@Id
	private Integer id;
	private String firstname;
	private String lastname;
	private String address;
	private Integer version;
	@Basic
	private String transientField;
	@OneToOne
	@JoinColumns({@JoinColumn(name = "busNumber_fk"), @JoinColumn(name = "busDriver_fk")})
	private BusTrip defaultBusTrip;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	@PostLoad
	public void calculate() {
		//...
	}
}
