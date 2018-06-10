/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.metamodel.wildcardmodel;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractOwner {

	@Id
	@GeneratedValue
	private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner", targetEntity = AbstractEntity.class)
	private List<? extends AbstractEntity> entities = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<? extends AbstractEntity> getEntities() {
		return entities;
	}

	public void setEntities(List<? extends AbstractEntity> entities) {
		this.entities = entities;
	}

}
