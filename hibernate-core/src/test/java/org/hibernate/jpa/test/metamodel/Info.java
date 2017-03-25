/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "INFO_TABLE")
public class Info implements java.io.Serializable {
	private String id;
	private String street;
	private String city;
	private String state;
	private String zip;
	private Spouse spouse;

	public Info() {
	}

	public Info(String v1, String v2, String v3, String v4, String v5) {
		id = v1;
		street = v2;
		city = v3;
		state = v4;
		zip = v5;
	}

	public Info(
			String v1, String v2, String v3, String v4,
			String v5, Spouse v6) {
		id = v1;
		street = v2;
		city = v3;
		state = v4;
		zip = v5;
		spouse = v6;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String v) {
		id = v;
	}

	@Column(name = "INFOSTREET")
	public String getStreet() {
		return street;
	}

	public void setStreet(String v) {
		street = v;
	}

	@Column(name = "INFOSTATE")
	public String getState() {
		return state;
	}

	public void setState(String v) {
		state = v;
	}

	@Column(name = "INFOCITY")
	public String getCity() {
		return city;
	}

	public void setCity(String v) {
		city = v;
	}

	@Column(name = "INFOZIP")
	public String getZip() {
		return zip;
	}

	public void setZip(String v) {
		zip = v;
	}

	@OneToOne(mappedBy = "info") @JoinTable( name = "INFO_SPOUSE_TABLE" )
	public Spouse getSpouse() {
		return spouse;
	}

	public void setSpouse(Spouse v) {
		this.spouse = v;
	}

}
