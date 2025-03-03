/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.registration;

import java.util.List;

import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.mapping.collections.custom.basic.IMyList;
import org.hibernate.orm.test.mapping.collections.custom.basic.MyList;
import org.hibernate.orm.test.mapping.collections.custom.basic.MyListType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.hibernate.metamodel.CollectionClassification.LIST;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
@Table(name = "`users`" )
@CollectionTypeRegistration( classification = LIST, type = MyListType.class )
public class User {
	@Id
	private Integer id;
	@NaturalId
	private String userName;
	@OneToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true )
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
