/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "GK_USER" )
public class User extends Principal {
	private String level;

	public User() {
	}

	public User(String uid) {
		super( uid );
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}
}
