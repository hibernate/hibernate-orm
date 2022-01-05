/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections.semantics;

import java.util.List;

import org.hibernate.annotations.CollectionSemantics;
import org.hibernate.annotations.CollectionType;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
//tag::collections-custom-type-ex[]
@Entity
public class AnotherEntityWithUniqueList {
	//end::collections-custom-type-ex[]
	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-custom-type-ex[]
	@ElementCollection
	@CollectionType(type = UniqueListType.class)
	private List<String> strings;

	// ...
	//end::collections-custom-type-ex[]

	private AnotherEntityWithUniqueList() {
		// for use by Hibernate
	}

	public AnotherEntityWithUniqueList(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getStrings() {
		return strings;
	}

	public void setStrings(List<String> strings) {
		this.strings = strings;
	}
//tag::collections-custom-type-ex[]
}
//end::collections-custom-type-ex[]

