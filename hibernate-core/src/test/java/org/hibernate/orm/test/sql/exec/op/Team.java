/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.op;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name="teams")
public class Team {
	@Id
	private Integer id;
	private String name;
	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumn(name = "team_fk")
	private Set<Person> members;

	public Team() {
	}

	public Team(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Person> getMembers() {
		return members;
	}

	public void setMembers(Set<Person> members) {
		this.members = members;
	}

	public Team addMember(Person member) {
		if ( members == null ) {
			members = new HashSet<>();
		}
		members.add( member );
		return this;
	}
}
