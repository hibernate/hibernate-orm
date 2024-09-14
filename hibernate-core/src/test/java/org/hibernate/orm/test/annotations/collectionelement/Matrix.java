/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.annotations.SortNatural;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity(name="Mtx")
public class Matrix {
	@Id
	@GeneratedValue
	@Column(name="mId")
	private Integer id;

	@ElementCollection
	@SortNatural
	@MapKeyColumn
	private SortedMap<Integer, Float> mvalues = new TreeMap<Integer, Float>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<Integer, Float> getMvalues() {
		return mvalues;
	}

	public void setMvalues(SortedMap<Integer, Float> mValues) {
		this.mvalues = mValues;
	}
}
