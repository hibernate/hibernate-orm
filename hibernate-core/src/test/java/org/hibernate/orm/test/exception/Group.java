/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exception;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "T_GROUP")
public class Group {
	@Id
	@GeneratedValue
	@Column(name = "group_id")
	private Long id;
	private String name;
	@ManyToMany(mappedBy = "memberships")
	private Set<User> members;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getMembers() {
		return members;
	}

	public void setMembers(Set members) {
		this.members = members;
	}

	public void addMember(User member) {
		if (member == null) {
			throw new IllegalArgumentException("Member to add cannot be null");
		}

		this.members.add(member);
		member.getMemberships().add(this);
	}
}
