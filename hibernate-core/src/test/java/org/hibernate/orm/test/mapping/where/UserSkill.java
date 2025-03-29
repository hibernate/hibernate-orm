/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.where;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static java.text.MessageFormat.format;
import static java.util.Objects.hash;

@SQLRestriction("has_deleted = false")
@Table(name = "t_user_skills")
@Entity(name = "UserSkill")
public class UserSkill {

	@Id
	@Column(name = "skill_id")
	private Integer id;

	@Column(name = "skill_name")
	private String skillName;

	@Column(name = "has_deleted")
	private Boolean deleted;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "user_fk")
	private User user;

	protected UserSkill() {
	}

	public UserSkill(Integer id, String skillName, Boolean deleted, User user) {
		this.id = id;
		this.skillName = skillName;
		this.deleted = deleted;
		this.user = user;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSkillName() {
		return skillName;
	}

	public void setSkillName(String skillName) {
		this.skillName = skillName;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		UserSkill that = (UserSkill) obj;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return hash(id);
	}

	@Override
	public String toString() {
		return format("UserSkill(id={0}, skillName={1}, deleted={2})", id, skillName, deleted);
	}
}
