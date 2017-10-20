/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfSets {
	private Integer id;
	private Set<String> setOfBasics;
	private Set<Component> setOfComponents;
	private Set<EntityOfSets> setOfOneToMany;
	private Set<EntityOfSets> setOfManyToMany;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ElementCollection
	public Set<String> getSetOfBasics() {
		return setOfBasics;
	}

	public void setSetOfBasics(Set<String> setOfBasics) {
		this.setOfBasics = setOfBasics;
	}

	@ElementCollection
	public Set<Component> getSetOfComponents() {
		return setOfComponents;
	}

	public void setSetOfComponents(Set<Component> setOfComponents) {
		this.setOfComponents = setOfComponents;
	}

	@OneToMany
	public Set<EntityOfSets> getSetOfOneToMany() {
		return setOfOneToMany;
	}

	public void setSetOfOneToMany(Set<EntityOfSets> setOfOneToMany) {
		this.setOfOneToMany = setOfOneToMany;
	}

	@ManyToMany
	public Set<EntityOfSets> getSetOfManyToMany() {
		return setOfManyToMany;
	}

	public void setSetOfManyToMany(Set<EntityOfSets> setOfManyToMany) {
		this.setOfManyToMany = setOfManyToMany;
	}

}
