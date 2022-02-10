/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

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
