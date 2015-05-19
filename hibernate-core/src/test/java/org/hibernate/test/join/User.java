/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;

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
