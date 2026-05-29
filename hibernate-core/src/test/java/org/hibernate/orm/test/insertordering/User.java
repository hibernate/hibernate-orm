/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "INS_ORD_USR")
public class User {
	@Id
	@GeneratedValue
	private Long id;
	@Column(name = "USR_NM")
	private String username;
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private Set<Membership> memberships = new HashSet<>();

	/**
	 * for persistence
	 */
	User() {
	}

	public User(String username) {
		this.username = username;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public Iterator getMemberships() {
		return memberships.iterator();
	}

	public Membership addMembership(Group group) {
		Membership membership = new Membership( this, group );
		memberships.add( membership );
		return membership;
	}
}
