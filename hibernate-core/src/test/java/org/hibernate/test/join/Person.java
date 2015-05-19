/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Transient;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn( name = "person_type" )
@DiscriminatorValue( value = "P" )
@SecondaryTable( name = "address", pkJoinColumns = @PrimaryKeyJoinColumn(name = "address_id") )
public class Person {
	private long id;
	private String name;
	private String address;
	private String zip;
	private String country;
	private double heightInches;
	private char sex;

	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public char getSex() {
		return sex;
	}

	public void setSex(char sex) {
		this.sex = sex;
	}

	public String getName() {
		return name;
	}

	public void setName(String identity) {
		this.name = identity;
	}

	@Transient
	public String getSpecies() {
		return null;
	}

	@Column( name = "height_centimeters" )
	@ColumnTransformer( read = "height_centimeters / 2.54E0", write = "? * 2.54E0" )
	public double getHeightInches() {
		return heightInches;
	}

	public void setHeightInches(double heightInches) {
		this.heightInches = heightInches;
	}

	@Column(table = "address")
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Column(table = "address")
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Column(table = "address")
	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}
}
