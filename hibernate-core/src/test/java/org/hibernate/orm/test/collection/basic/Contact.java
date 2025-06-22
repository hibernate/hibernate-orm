/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.basic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="contact")
public class Contact implements Serializable {

	private static final long serialVersionUID = 1L;
	private Long id;
	private String name;
	private Set<EmailAddress> emailAddresses = new HashSet<>();
	private Set<EmailAddress> emailAddresses2 = new HashSet<>();
	private Map<EmailAddress,Contact> contactsByEmail = new HashMap<>();

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Basic
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ElementCollection
	@CollectionTable(name = "user_email_addresses2", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
	public Set<EmailAddress> getEmailAddresses2() {
		return emailAddresses2;
	}

	public void setEmailAddresses2(Set<EmailAddress> emailAddresses2) {
		this.emailAddresses2 = emailAddresses2;
	}

	@ElementCollection
	@CollectionTable(name = "user_email_addresses", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
	public Set<EmailAddress> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(Set<EmailAddress> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	@ManyToMany
	@CollectionTable(name="contact_email_addresses")
	public Map<EmailAddress, Contact> getContactsByEmail() {
		return contactsByEmail;
	}

	public void setContactsByEmail(Map<EmailAddress, Contact> contactsByEmail) {
		this.contactsByEmail = contactsByEmail;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += (id != null ? id.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Contact)) {
			return false;
		}
		final Contact other = (Contact) obj;
		if (this.id == null || other.id == null) {
			return this == obj;
		}
		if(!this.id.equals(other.id)) {
			return this == obj;
		}
		return true;
	}

	@Override
	public String toString() {
		return "com.clevercure.web.hibernateissuecache.User[ id=" + id + " ]";
	}
}
