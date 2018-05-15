/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfSets {
	private Integer id;
	private Set<String> setOfBasics;
	private Set<Component> setOfComponents;
	private Set<Component> setOfExtraLazyComponents;
	private Set<EntityOfSets> setOfOneToMany;
	private Set<EntityOfSets> setOfManyToMany;

	public EntityOfSets() {
	}

	public EntityOfSets(Integer id) {
		this.id = id;
		this.setOfBasics = new HashSet<>();
		this.setOfComponents = new HashSet<>();
		this.setOfOneToMany = new HashSet<>();
		this.setOfManyToMany = new HashSet<>();
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ElementCollection()
//	@ElementCollection( fetch = FetchType.EAGER )
	@CollectionTable( name = "EntityOfSet_basics")
	public Set<String> getSetOfBasics() {
		return setOfBasics;
	}

	public void setSetOfBasics(Set<String> setOfBasics) {
		this.setOfBasics = setOfBasics;
	}

	@ElementCollection
	@CollectionTable( name = "EntityOfSet_components")
	public Set<Component> getSetOfComponents() {
		return setOfComponents;
	}

	public void setSetOfComponents(Set<Component> setOfComponents) {
		this.setOfComponents = setOfComponents;
	}

	@ElementCollection
	@LazyCollection( LazyCollectionOption.EXTRA )
	@CollectionTable( name = "EntityOfSet_extraLazyComponents")
	public Set<Component> getSetOfExtraLazyComponents() {
		return setOfExtraLazyComponents;
	}

	public void setSetOfExtraLazyComponents(Set<Component> setOfExtraLazyComponents) {
		this.setOfExtraLazyComponents = setOfExtraLazyComponents;
	}

	@OneToMany
	@CollectionTable( name = "EntityOfSet_oneToMany")
	public Set<EntityOfSets> getSetOfOneToMany() {
		return setOfOneToMany;
	}

	public void setSetOfOneToMany(Set<EntityOfSets> setOfOneToMany) {
		this.setOfOneToMany = setOfOneToMany;
	}

	@ManyToMany
	@CollectionTable( name = "EntityOfSet_manyToMany")
	public Set<EntityOfSets> getSetOfManyToMany() {
		return setOfManyToMany;
	}

	public void setSetOfManyToMany(Set<EntityOfSets> setOfManyToMany) {
		this.setOfManyToMany = setOfManyToMany;
	}

}
