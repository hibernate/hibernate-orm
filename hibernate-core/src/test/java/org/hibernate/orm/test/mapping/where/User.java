/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.where;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static java.util.Objects.hash;

@NamedEntityGraph(
		name = "user-entity-graph",
		attributeNodes = {
				@NamedAttributeNode(value = "detail"),
				@NamedAttributeNode(value = "skills")
		}
)
@Table(name = "t_users")
@Entity(name = "User")
public class User {

	@Id
	@Column(name = "user_id")
	private Integer id;

	@Column(name = "user_name")
	private String name;

	@OneToOne(mappedBy = "user", fetch = LAZY)
	private UserDetail detail;

	@OneToMany(mappedBy = "user", fetch = LAZY)
	private Set<UserSkill> skills = new HashSet<>();

	protected User() {
	}

	public User(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public User(String name, UserDetail detail) {
		this.name = name;
		this.detail = detail;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UserDetail getDetail() {
		return detail;
	}

	public void setDetail(UserDetail detail) {
		this.detail = detail;
	}

	public Set<UserSkill> getSkills() {
		return skills;
	}

	public void setSkills(Set<UserSkill> skills) {
		this.skills = skills;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		User that = (User) obj;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return hash(id);
	}

//	@Override
//	public String toString() {
//		return format("User(id={0}, name={1}, detail={2}, skills=[{3}])",
//				id, name, detail, skills.stream().map(UserSkill::toString).collect(joining(", ")));
//	}
}
