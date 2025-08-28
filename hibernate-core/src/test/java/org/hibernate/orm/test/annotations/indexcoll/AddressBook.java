/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class AddressBook {
	private Integer id;
	private String owner;
	private Map<AddressEntryPk, AddressEntry> entries = new HashMap<AddressEntryPk, AddressEntry>();
	private Map<String, AddressEntry> lastNameEntries = new HashMap<String, AddressEntry>();
	private Map<AlphabeticalDirectory, AddressEntry> directoryEntries = new HashMap<AlphabeticalDirectory, AddressEntry>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@MapKey
	@OneToMany(mappedBy = "book", cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE})
	public Map<AddressEntryPk, AddressEntry> getEntries() {
		return entries;
	}

	public void setEntries(Map<AddressEntryPk, AddressEntry> entries) {
		this.entries = entries;
	}

	@MapKey(name = "person.lastname")
	@OneToMany(mappedBy = "book")
	public Map<String, AddressEntry> getLastNameEntries() {
		return lastNameEntries;
	}

	public void setLastNameEntries(Map<String, AddressEntry> lastNameEntries) {
		this.lastNameEntries = lastNameEntries;
	}

	@MapKey(name = "directory")
	@OneToMany(mappedBy = "book")
	public Map<AlphabeticalDirectory, AddressEntry> getDirectoryEntries() {
		return directoryEntries;
	}

	public void setDirectoryEntries(Map<AlphabeticalDirectory, AddressEntry> directoryEntries) {
		this.directoryEntries = directoryEntries;
	}

}
