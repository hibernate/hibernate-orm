/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import org.hibernate.annotations.CollectionType;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
@Table(name = "UC_BSC_USER")
public class UserWithUnimplementedCollection {
	private String userName;
	private AtomicReference<Email> emailAddresses = new AtomicReference<Email>();
	private Map sessionData = new HashMap();

	UserWithUnimplementedCollection() {

	}
	public UserWithUnimplementedCollection(String name) {
		userName = name;
	}

	@Id
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}

	@OneToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true )
	@CollectionType(type = HeadListType.class )
	@JoinColumn( name = "userName" )
	@OrderColumn( name = "displayOrder" )
	public AtomicReference<Email> getEmailAddresses() {  //can declare a custom interface type
		return emailAddresses;
	}
	public void setEmailAddresses(AtomicReference<Email> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	@Transient
	public Map getSessionData() {
		return sessionData;
	}
	public void setSessionData(Map sessionData) {
		this.sessionData = sessionData;
	}
}
