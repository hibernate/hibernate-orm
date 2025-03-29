/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.ordered;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OrderBy;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Person {
	private Long id;
	private String name;

	private Set<String> nickNamesAscendingNaturalSort = new HashSet<>();
	private Set<String> nickNamesDescendingNaturalSort = new HashSet<>();

	private Set<Address> addressesAscendingNaturalSort = new HashSet<>();
	private Set<Address> addressesDescendingNaturalSort = new HashSet<>();
	private Set<Address> addressesCityAscendingSort = new HashSet<>();
	private Set<Address> addressesCityDescendingSort = new HashSet<>();


	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	@ElementCollection
	@JoinTable(name = "T_NICKNAMES_A")
	@OrderBy // testing default @OrderBy mapping
	public Set<String> getNickNamesAscendingNaturalSort() {
		return nickNamesAscendingNaturalSort;
	}

	public void setNickNamesAscendingNaturalSort(Set<String> nickNamesAscendingNaturalSort) {
		this.nickNamesAscendingNaturalSort = nickNamesAscendingNaturalSort;
	}

	@ElementCollection
	@JoinTable(name = "T_NICKNAMES_D")
	@OrderBy( "desc" )
	public Set<String> getNickNamesDescendingNaturalSort() {
		return nickNamesDescendingNaturalSort;
	}

	public void setNickNamesDescendingNaturalSort(Set<String> nickNamesDescendingNaturalSort) {
		this.nickNamesDescendingNaturalSort = nickNamesDescendingNaturalSort;
	}


	@ElementCollection
	@OrderBy
	@JoinTable(name = "T_ADDRESS_A")
	public Set<Address> getAddressesAscendingNaturalSort() {
		return addressesAscendingNaturalSort;
	}

	public void setAddressesAscendingNaturalSort(Set<Address> addressesAscendingNaturalSort) {
		this.addressesAscendingNaturalSort = addressesAscendingNaturalSort;
	}

	@ElementCollection
	@OrderBy( "desc" )
	@JoinTable(name = "T_ADDRESS_D")
	public Set<Address> getAddressesDescendingNaturalSort() {
		return addressesDescendingNaturalSort;
	}

	public void setAddressesDescendingNaturalSort(Set<Address> addressesDescendingNaturalSort) {
		this.addressesDescendingNaturalSort = addressesDescendingNaturalSort;
	}

	@ElementCollection
	@OrderBy( "city" )
	@JoinTable(name = "T_ADD_CITY_A")
	public Set<Address> getAddressesCityAscendingSort() {
		return addressesCityAscendingSort;
	}

	public void setAddressesCityAscendingSort(Set<Address> addressesCityAscendingSort) {
		this.addressesCityAscendingSort = addressesCityAscendingSort;
	}

	@ElementCollection
	@OrderBy( "city desc" )
	@JoinTable(name = "T_ADD_CITY_D")
	public Set<Address> getAddressesCityDescendingSort() {
		return addressesCityDescendingSort;
	}

	public void setAddressesCityDescendingSort(Set<Address> addressesCityDescendingSort) {
		this.addressesCityDescendingSort = addressesCityDescendingSort;
	}
}
