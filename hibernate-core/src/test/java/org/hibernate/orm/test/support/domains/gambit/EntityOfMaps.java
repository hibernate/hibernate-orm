/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfMaps {
	private Integer id;
	private Map<String,String> basicToBasicMap;
	private Map<String,Component> basicToComponentMap;
	private Map<Component,String> componentToBasicMap;
	private Map<String,EntityOfMaps> basicToOneToMany;
	private Map<String,EntityOfMaps> basicToManyToMany;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ElementCollection
	public Map<String, String> getBasicToBasicMap() {
		return basicToBasicMap;
	}

	public void setBasicToBasicMap(Map<String, String> basicToBasicMap) {
		this.basicToBasicMap = basicToBasicMap;
	}

	@ElementCollection
	public Map<String, Component> getBasicToComponentMap() {
		return basicToComponentMap;
	}

	public void setBasicToComponentMap(Map<String, Component> basicToComponentMap) {
		this.basicToComponentMap = basicToComponentMap;
	}

	@ElementCollection
	public Map<Component, String> getComponentToBasicMap() {
		return componentToBasicMap;
	}

	public void setComponentToBasicMap(Map<Component, String> componentToBasicMap) {
		this.componentToBasicMap = componentToBasicMap;
	}

	@OneToMany
	@JoinColumn
	public Map<String, EntityOfMaps> getBasicToOneToMany() {
		return basicToOneToMany;
	}

	public void setBasicToOneToMany(Map<String, EntityOfMaps> basicToOneToMany) {
		this.basicToOneToMany = basicToOneToMany;
	}

	@ManyToMany
	public Map<String, EntityOfMaps> getBasicToManyToMany() {
		return basicToManyToMany;
	}

	public void setBasicToManyToMany(Map<String, EntityOfMaps> basicToManyToMany) {
		this.basicToManyToMany = basicToManyToMany;
	}

}
