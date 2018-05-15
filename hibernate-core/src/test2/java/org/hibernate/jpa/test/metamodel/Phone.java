/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "PHONE_TABLE")
public class Phone implements java.io.Serializable {
	public enum Type { LAND_LINE, CELL, FAX, WORK, HOME }

	private String id;
	private String area;
	private String number;
	private Address address;
	private Set<Type> types;

	public Phone() {
	}

	public Phone(String v1, String v2, String v3) {
		id = v1;
		area = v2;
		number = v3;
	}

	public Phone(String v1, String v2, String v3, Address v4) {
		id = v1;
		area = v2;
		number = v3;
		address = v4;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String v) {
		id = v;
	}

	@Column(name = "AREA")
	public String getArea() {
		return area;
	}

	public void setArea(String v) {
		area = v;
	}

	@Column(name = "PHONE_NUMBER")
	public String getNumber() {
		return number;
	}

	public void setNumber(String v) {
		number = v;
	}

	@ManyToOne
	@JoinColumn(name = "FK_FOR_ADDRESS")
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address a) {
		address = a;
	}

	@ElementCollection
	public Set<Type> getTypes() {
		return types;
	}

	public void setTypes(Set<Type> types) {
		this.types = types;
	}
}