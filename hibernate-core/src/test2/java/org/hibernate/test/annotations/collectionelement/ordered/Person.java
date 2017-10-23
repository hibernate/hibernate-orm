/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement.ordered;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
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
	@JoinTable(name = "T_NICKNAMES_A")
	@OrderBy // testing default @OrderBy mapping
	public Set<String> getNickNamesAscendingNaturalSort() {
		return nickNamesAscendingNaturalSort;
	}

	public void setNickNamesAscendingNaturalSort(Set<String> nickNamesAscendingNaturalSort) {
		this.nickNamesAscendingNaturalSort = nickNamesAscendingNaturalSort;
	}

	@ElementCollection
	@JoinColumn
	@JoinTable(name = "T_NICKNAMES_D")
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
	@JoinTable(name = "T_ADDRESS_A")
	public Set<Address> getAddressesAscendingNaturalSort() {
		return addressesAscendingNaturalSort;
	}

	public void setAddressesAscendingNaturalSort(Set<Address> addressesAscendingNaturalSort) {
		this.addressesAscendingNaturalSort = addressesAscendingNaturalSort;
	}

	@ElementCollection
	@JoinColumn
	@OrderBy( "desc" )
	@JoinTable(name = "T_ADDRESS_D")
	public Set<Address> getAddressesDescendingNaturalSort() {
		return addressesDescendingNaturalSort;
	}

	public void setAddressesDescendingNaturalSort(Set<Address> addressesDescendingNaturalSort) {
		this.addressesDescendingNaturalSort = addressesDescendingNaturalSort;
	}

	@ElementCollection
	@JoinColumn
	@OrderBy( "city" )
	@JoinTable(name = "T_ADD_CITY_A")
	public Set<Address> getAddressesCityAscendingSort() {
		return addressesCityAscendingSort;
	}

	public void setAddressesCityAscendingSort(Set<Address> addressesCityAscendingSort) {
		this.addressesCityAscendingSort = addressesCityAscendingSort;
	}

	@ElementCollection
	@JoinColumn
	@OrderBy( "city desc" )
	@JoinTable(name = "T_ADD_CITY_D")
	public Set<Address> getAddressesCityDescendingSort() {
		return addressesCityDescendingSort;
	}

	public void setAddressesCityDescendingSort(Set<Address> addressesCityDescendingSort) {
		this.addressesCityDescendingSort = addressesCityDescendingSort;
	}
}
