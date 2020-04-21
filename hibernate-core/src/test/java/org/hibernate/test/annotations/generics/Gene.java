/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.generics;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Type;

/**
 * @author Paolo Perrotta
 */
@Entity
public class Gene<T, STE extends Enum> {

	private Integer id;
	private STE state;

	@Type(type="org.hibernate.test.annotations.generics.StateType")
	public STE getState() {
		return state;
	}

	public void setState(STE state) {
		this.state = state;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = DNA.class)
	public T getGeneticCode() {
		return null;
	}

	public void setGeneticCode(T gene) {
	}
}
