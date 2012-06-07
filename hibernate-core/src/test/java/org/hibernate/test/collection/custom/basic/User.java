/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) ${year}, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.collection.custom.basic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CollectionType;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
@Table(name = "UC_BSC_USER")
public class User {
	private String userName;
	private IMyList<Email> emailAddresses = new MyList<Email>();
	private Map sessionData = new HashMap();

	User() {

	}
	public User(String name) {
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
	@CollectionType( type = "org.hibernate.test.collection.custom.basic.MyListType" )
	@JoinColumn( name = "userName" )
	@OrderColumn( name = "displayOrder" )
	public List<Email> getEmailAddresses() {
// does not work :(
//	public IMyList<Email> getEmailAddresses() {
		return emailAddresses;
	}
	public void setEmailAddresses(IMyList<Email> emailAddresses) {
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
