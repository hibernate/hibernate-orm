/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.propertyref.lazy;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


@Entity
@Table(name = "t_user")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Integer id;

	@Column(name = "userid", length = 32, nullable = false)
	private String userid;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
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
