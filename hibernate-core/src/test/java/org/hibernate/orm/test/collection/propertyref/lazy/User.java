/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.propertyref.lazy;

import java.util.HashSet;
import java.util.Set;


public class User {

	private Integer id;
	private String userid;
	private Set<Mail> mail = new HashSet();

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

	public Set<Mail> getMail() {
		return mail;
	}

	private void setMail(Set<Mail> mail) {
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
