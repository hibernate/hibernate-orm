//$Id$
package org.hibernate.test.annotations.reflection;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

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
