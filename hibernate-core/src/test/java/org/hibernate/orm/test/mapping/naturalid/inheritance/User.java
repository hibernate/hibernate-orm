/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "GK_USER" )
public class User extends Principal {
	private String level;

	public User() {
	}

	public User(String uid) {
		super( uid );
	}

	@Column(name = "user_level")
	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}
}
