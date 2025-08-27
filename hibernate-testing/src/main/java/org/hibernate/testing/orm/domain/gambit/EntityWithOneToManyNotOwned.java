/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Chris Cranford
 */
@Entity
public class EntityWithOneToManyNotOwned {
	private Integer id;
	private List<EntityWithManyToOneWithoutJoinTable> children = new ArrayList<>();

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(mappedBy = "owner")
	public List<EntityWithManyToOneWithoutJoinTable> getChildren() {
		return children;
	}

	public void setChildren(List<EntityWithManyToOneWithoutJoinTable> children) {
		this.children = children;
	}

	public void addChild(EntityWithManyToOneWithoutJoinTable child) {
		child.setOwner( this );
		getChildren().add( child );
	}
}
