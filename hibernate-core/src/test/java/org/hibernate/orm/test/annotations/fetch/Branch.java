/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetch;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Branch {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToMany(mappedBy = "branch", fetch = FetchType.EAGER )
	private Set<Leaf> leaves = new HashSet<Leaf>();


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Leaf> getLeaves() {
		return leaves;
	}

	public void setLeaves(Set<Leaf> leaves) {
		this.leaves = leaves;
	}
}
