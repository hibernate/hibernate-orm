/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Transient;

import org.hibernate.annotations.ColumnTransformer;
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
