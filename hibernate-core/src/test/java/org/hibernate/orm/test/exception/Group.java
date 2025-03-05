/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exception;
import java.util.Set;

/**
 * Implementation of Group.
 *
 * @author Steve Ebersole
 */
public class Group {
	private Long id;
	private String name;
	private Set members;

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
