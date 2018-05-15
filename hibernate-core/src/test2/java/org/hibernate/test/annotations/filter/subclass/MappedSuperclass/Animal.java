/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.filter.subclass.MappedSuperclass;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(name = "nameFilter", parameters = @ParamDef(name = "name", type = "string"))
@Filter(name = "nameFilter", condition = ":name = ANIMAL_NAME")
public abstract class Animal {
	@Id
	@GeneratedValue
	@Column(name = "ANIMAL_ID")
	private Integer id;

	@Column(name = "ANIMAL_NAME")
	private String name;

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

}
