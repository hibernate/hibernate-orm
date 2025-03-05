/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.secondarytable;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.type.NumericBooleanConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

@Entity
@Table(name="T_USER")
@SecondaryTable(name="SECURITY_USER")
@FilterDef(name="ageFilter", parameters=@ParamDef(name="age", type=Integer.class))
@Filter(name="ageFilter", condition="{u}.AGE < :age AND {s}.LOCKED_OUT <> 1",
				aliases={@SqlFragmentAlias(alias="u", table="T_USER"), @SqlFragmentAlias(alias="s", table="SECURITY_USER")})
public class User {

	@Id
	@GeneratedValue
	@Column(name="USER_ID")
	private int id;

	@Column(name="EMAIL_ADDRESS")
	private String emailAddress;

	@Column(name="AGE")
	private int age;

	@Column(name="SECURITY_USERNAME", table="SECURITY_USER")
	private String username;

	@Column(name="SECURITY_PASSWORD", table="SECURITY_USER")
	private String password;

	@Column(name="LOCKED_OUT", table="SECURITY_USER")
	@Convert( converter = NumericBooleanConverter.class )
	private boolean lockedOut;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isLockedOut() {
		return lockedOut;
	}

	public void setLockedOut(boolean lockedOut) {
		this.lockedOut = lockedOut;
	}

}
