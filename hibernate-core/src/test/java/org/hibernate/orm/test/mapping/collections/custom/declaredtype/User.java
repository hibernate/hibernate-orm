/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import org.hibernate.annotations.CollectionType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
@Table(name = "UC_BSC_USER")
public class User {
	@Id
	private Integer id;
	private String userName;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@CollectionType(type = HeadListType.class )
	@JoinColumn(name = "userName")
	@OrderColumn(name = "displayOrder")
	private IHeadList<Email> emailAddresses = new HeadList<Email>();

	private User() {
	}

	public User(Integer id, String name) {
		this.id = id;
		this.userName = name;
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

	public IHeadList<Email> getEmailAddresses() {  //can declare a custom interface type
		return emailAddresses;
	}

	public void setEmailAddresses(IHeadList<Email> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}
}
