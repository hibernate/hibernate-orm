// $Id: Group.java 7805 2005-08-10 16:25:11Z steveebersole $
package org.hibernate.test.version.sybase;
import java.util.Date;
import java.util.Set;

/**
 * Implementation of Group.
 *
 * @author Steve Ebersole
 */
public class Group {
	private Long id;
	private Date timestamp;
	private String name;
	private Set users;

	public Group() {
	}

	public Group(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getUsers() {
		return users;
	}

	public void setUsers(Set users) {
		this.users = users;
	}
}
