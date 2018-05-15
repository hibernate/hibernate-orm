/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter.hql;


/**
 * Non-leaf subclass
 *
 * @author Steve Ebersole
 */
public class User extends Person {
	private String username;

	protected User() {
		super();
	}

	public User(String name, char sex, String username) {
		super( name, sex );
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
