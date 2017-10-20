/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfLists {
	private Integer id;
	private List<String> listOfBasics;
	private List<Component> listOfComponents;
	private List<EntityOfLists> listOfOneToMany;
	private List<EntityOfLists> listOfManyToMany;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ElementCollection
	@OrderColumn
	public List<String> getListOfBasics() {
		return listOfBasics;
	}

	public void setListOfBasics(List<String> listOfBasics) {
		this.listOfBasics = listOfBasics;
	}

	@ElementCollection
	@OrderColumn
	public List<Component> getListOfComponents() {
		return listOfComponents;
	}

	public void setListOfComponents(List<Component> listOfComponents) {
		this.listOfComponents = listOfComponents;
	}

	@OneToMany
	@OrderColumn
	public List<EntityOfLists> getListOfOneToMany() {
		return listOfOneToMany;
	}

	public void setListOfOneToMany(List<EntityOfLists> listOfOneToMany) {
		this.listOfOneToMany = listOfOneToMany;
	}

	@ManyToMany
	@OrderColumn
	public List<EntityOfLists> getListOfManyToMany() {
		return listOfManyToMany;
	}

	public void setListOfManyToMany(List<EntityOfLists> listOfManyToMany) {
		this.listOfManyToMany = listOfManyToMany;
	}
}
