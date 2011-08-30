/*
 * User.java
 *
 * Created on May 3, 2005, 9:42 AM
 */

package org.hibernate.test.orphan;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Kevin
 */
public class User {

	private Integer id;
	private String userid;
	private Set mail = new HashSet();

	public User() {
	}

	public User(String userid) {
		this.userid = userid;
	}

	public Integer getId() {
		return id;
	}

	protected void setId(Integer id) {
		this.id = id;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public Set getMail() {
		return mail;
	}

	private void setMail(Set mail) {
		this.mail = mail;
	}

	public Mail addMail(String alias) {
		Mail mail = new Mail( alias, this );
		getMail().add( mail );
		return mail;
	}

	public void removeMail(Mail mail) {
		getMail().remove( mail );
	}
}
