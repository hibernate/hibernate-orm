/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.basic;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.NaturalId;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
@Table(name = "UC_BSC_USER")
public class User {
	@Id
	private Integer id;
	@NaturalId
	private String userName;
	@OneToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true )
	@CollectionType( type = MyListType.class )
	@JoinColumn( name = "userName" )
	@OrderColumn( name = "displayOrder" )
	private IMyList<Email> emailAddresses = new MyList<>();

	private User() {
		// for use by Hibernate
	}

	public User(Integer id, String name) {
		this.id = id;
		userName = name;
	}

	public Integer getId() {
		return id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public List<Email> getEmailAddresses() {
		return emailAddresses;
	}
	public void setEmailAddresses(IMyList<Email> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}
}
