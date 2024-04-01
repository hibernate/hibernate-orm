/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import java.util.List;

/**
 * An entity with multiple attributes of basic type for use in testing using those types/attributes
 * in queries.
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name="ENT_W_MANY_COLS")
public class MultiTypedBasicAttributesEntity {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	private Long id;
	private int[] someInts;
	private Integer[] someWrappedIntegers;

	private List<Integer> integerList;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int[] getSomeInts() {
		return someInts;
	}

	public void setSomeInts(int[] someInts) {
		this.someInts = someInts;
	}

	public Integer[] getSomeWrappedIntegers() {
		return someWrappedIntegers;
	}

	public void setSomeWrappedIntegers(Integer[] someWrappedIntegers) {
		this.someWrappedIntegers = someWrappedIntegers;
	}

	public List<Integer> getIntegerList() {
		return integerList;
	}

	public void setIntegerList(List<Integer> integerList) {
		this.integerList = integerList;
	}
}
