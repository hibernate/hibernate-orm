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
