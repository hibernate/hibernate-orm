/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderBy;

@SuppressWarnings({"unchecked", "serial"})

@Entity
public class BugSystem {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@OrderBy("reportedBy.lastName ASC,reportedBy.firstName ASC,summary")
	private Set<Bug> bugs;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Bug> getBugs() {
		return bugs;
	}

	public void setBugs(Set<Bug> bugs) {
		this.bugs = bugs;
	}

}
