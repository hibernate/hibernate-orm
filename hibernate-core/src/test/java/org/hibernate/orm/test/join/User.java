/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;

import org.hibernate.annotations.ColumnTransformer;

/**
 * @author Mike Dillon
 * @author Steve Ebersole
 */
@Entity
@DiscriminatorValue( "U" )
@SecondaryTables({
		@SecondaryTable(name = "t_user"),
		@SecondaryTable(name = "t_silly")
})
public class User extends Person {
	private String login;
	private Double passwordExpiryDays;
	private String silly;

	@Column(table = "t_user", name = "u_login")
	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	@Column(table = "t_user", name = "pwd_expiry_weeks")
	@ColumnTransformer( read = "pwd_expiry_weeks * 7.0E0", write = "? / 7.0E0")
	public Double getPasswordExpiryDays() {
		return passwordExpiryDays;
	}

	public void setPasswordExpiryDays(Double passwordExpiryDays) {
		this.passwordExpiryDays = passwordExpiryDays;
	}

	@Column(table = "t_silly")
	public String getSilly() {
		return silly;
	}

	public void setSilly(String silly) {
		this.silly = silly;
	}
}
