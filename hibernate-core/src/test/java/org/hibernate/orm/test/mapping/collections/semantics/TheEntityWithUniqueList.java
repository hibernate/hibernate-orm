/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections.semantics;

import java.util.List;

import org.hibernate.annotations.CollectionSemantics;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Table(name = "unique_list_owners")
//tag::collections-custom-semantics-ex[]
@Entity
public class TheEntityWithUniqueList {
	//end::collections-custom-semantics-ex[]
	@Id
	private Integer id;
	@Basic
	private String name;

	@CollectionTable(name = "unique_list_contents")
	//tag::collections-custom-semantics-ex[]
	@ElementCollection
	@CollectionSemantics(UniqueListSemantic.class)
	private List<String> strings;

	// ...
	//end::collections-custom-semantics-ex[]

	private TheEntityWithUniqueList() {
		// for use by Hibernate
	}

	public TheEntityWithUniqueList(Integer id, String name) {
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
//tag::collections-custom-semantics-ex[]
}
//end::collections-custom-semantics-ex[]

