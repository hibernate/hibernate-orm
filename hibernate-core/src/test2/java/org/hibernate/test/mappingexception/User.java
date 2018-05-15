/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: User.java 4746 2004-11-11 20:57:28Z steveebersole $
package org.hibernate.test.mappingexception;



/**
 * 
 *
 * @author Max Rydahl Andersen
 */
public class User {
	private Long id;
	private String username;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
