/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

/**
 * @author Koen Aers
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfArrays {

	private Integer id;
	private String name;

	private String[] arrayOfBasics;


	public EntityOfArrays() {
	}

	public EntityOfArrays(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// arrayOfBasics

	@ElementCollection
	@OrderColumn
	public String[] getArrayOfBasics() {
		return arrayOfBasics;
	}

	public void setArrayOfBasics(String[] arrayOfBasics) {
		this.arrayOfBasics = arrayOfBasics;
	}

}
