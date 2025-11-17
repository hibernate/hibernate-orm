/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.union;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "FolderUnion")
public class Folder extends File {
	private Set<File> children = new HashSet<File>();

	Folder() {
	}

	public Folder(String name) {
		super( name );
	}

	@OneToMany(mappedBy = "parent")
	public Set<File> getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}
}
