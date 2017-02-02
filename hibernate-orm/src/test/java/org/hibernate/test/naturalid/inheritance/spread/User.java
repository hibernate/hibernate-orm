/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.inheritance.spread;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "GK_USER" )
public class User extends Principal {
	private String userName;

	public User() {
	}

	public User(String uid) {
		super( uid );
		// stupid, but this is just to test the "spreading" declaration of natural id, not whether these particular
		// values make sense :)
		this.userName = uid;
	}

	@NaturalId
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
