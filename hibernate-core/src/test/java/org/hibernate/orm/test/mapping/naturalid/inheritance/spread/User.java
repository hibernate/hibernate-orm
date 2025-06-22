/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance.spread;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "GK_USER" )
public class User extends Principal {
	private String userName;

	public User() {
	}

	public User(String uid) {
		super( uid );
		// stupid, but this is just to test the "spreading" declaration of natural id, not whether these particular
		// values make sense :)
		this.userName = uid;
	}

	@NaturalId
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
