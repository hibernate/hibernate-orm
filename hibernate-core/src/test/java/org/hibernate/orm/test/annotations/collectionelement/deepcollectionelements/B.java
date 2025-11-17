/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.deepcollectionelements;
import java.util.List;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

@Embeddable
public class B {
	private String name;

	//@CollectionOfElements
	@OneToMany
	@OrderColumn( name = "ndx" )
	private List<C> listOfC;

	public List<C> getListOfC() {
		return listOfC;
	}

	public void setListOfC(List<C> listOfC) {
		this.listOfC = listOfC;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
