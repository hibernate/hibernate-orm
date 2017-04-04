/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.filter.secondarytable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.Type;

@Entity
@Table(name="T_USER")
@SecondaryTable(name="SECURITY_USER")
@FilterDef(name="ageFilter", parameters=@ParamDef(name="age", type="integer"))
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
	@Type( type = "numeric_boolean")
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
