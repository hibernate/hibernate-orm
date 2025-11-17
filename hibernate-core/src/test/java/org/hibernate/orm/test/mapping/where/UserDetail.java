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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static java.text.MessageFormat.format;
import static java.util.Objects.hash;

@SQLRestriction("is_active = true")
@Table(name = "t_user_details")
@Entity(name = "UserDetail")
public class UserDetail {

	@Id
	@Column(name = "detail_id")
	private Integer id;

	@Column(name = "city")
	private String city;

	@Column(name = "is_active")
	private Boolean active;

	@OneToOne(fetch = LAZY)
	@JoinColumn(name = "user_fk")
	private User user;

	protected UserDetail() {
	}

	public UserDetail(Integer id, String city, Boolean active, User user) {
		this.id = id;
		this.city = city;
		this.active = active;
		this.user = user;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
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
		UserDetail that = (UserDetail) obj;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return hash(id);
	}

	@Override
	public String toString() {
		return format("UserDetail(id={0}, city={1}, active={2})", id, city, active);
	}
}
