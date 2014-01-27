/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.collectionelement.ordered;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderBy;

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

	private Set<String> nickNamesAscendingNaturalSort = new HashSet<String>();
	private Set<String> nickNamesDescendingNaturalSort = new HashSet<String>();

	private Set<Address> addressesAscendingNaturalSort = new HashSet<Address>();
	private Set<Address> addressesDescendingNaturalSort = new HashSet<Address>();
	private Set<Address> addressesCityAscendingSort = new HashSet<Address>();
	private Set<Address> addressesCityDescendingSort = new HashSet<Address>();


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
	@JoinColumn
	@CollectionTable(name = "T_NICKNAMES_A")
	@OrderBy // testing default @OrderBy mapping
	public Set<String> getNickNamesAscendingNaturalSort() {
		return nickNamesAscendingNaturalSort;
	}

	public void setNickNamesAscendingNaturalSort(Set<String> nickNamesAscendingNaturalSort) {
		this.nickNamesAscendingNaturalSort = nickNamesAscendingNaturalSort;
	}

	@ElementCollection
	@JoinColumn
	@CollectionTable(name = "T_NICKNAMES_D")
	@OrderBy( "desc" )
	public Set<String> getNickNamesDescendingNaturalSort() {
		return nickNamesDescendingNaturalSort;
	}

	public void setNickNamesDescendingNaturalSort(Set<String> nickNamesDescendingNaturalSort) {
		this.nickNamesDescendingNaturalSort = nickNamesDescendingNaturalSort;
	}


	@ElementCollection
	@JoinColumn
	@OrderBy
	@CollectionTable(name = "T_ADDRESS_A")
	public Set<Address> getAddressesAscendingNaturalSort() {
		return addressesAscendingNaturalSort;
	}

	public void setAddressesAscendingNaturalSort(Set<Address> addressesAscendingNaturalSort) {
		this.addressesAscendingNaturalSort = addressesAscendingNaturalSort;
	}

	@ElementCollection
	@JoinColumn
	@OrderBy( "desc" )
	@CollectionTable(name = "T_ADDRESS_D")
	public Set<Address> getAddressesDescendingNaturalSort() {
		return addressesDescendingNaturalSort;
	}

	public void setAddressesDescendingNaturalSort(Set<Address> addressesDescendingNaturalSort) {
		this.addressesDescendingNaturalSort = addressesDescendingNaturalSort;
	}

	@ElementCollection
	@JoinColumn
	@OrderBy( "city" )
	@CollectionTable(name = "T_ADD_CITY_A")
	public Set<Address> getAddressesCityAscendingSort() {
		return addressesCityAscendingSort;
	}

	public void setAddressesCityAscendingSort(Set<Address> addressesCityAscendingSort) {
		this.addressesCityAscendingSort = addressesCityAscendingSort;
	}

	@ElementCollection
	@JoinColumn
	@OrderBy( "city desc" )
	@CollectionTable(name = "T_ADD_CITY_D")
	public Set<Address> getAddressesCityDescendingSort() {
		return addressesCityDescendingSort;
	}

	public void setAddressesCityDescendingSort(Set<Address> addressesCityDescendingSort) {
		this.addressesCityDescendingSort = addressesCityDescendingSort;
	}
}
