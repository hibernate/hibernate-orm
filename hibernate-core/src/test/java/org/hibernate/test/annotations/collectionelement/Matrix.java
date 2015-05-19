/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.collectionelement;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;

import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Type;

/**
 * @author Emmanuel Bernard
 */
@Entity(name="Mtx")
public class Matrix {
	@Id
	@GeneratedValue
	@Column(name="mId")
	private Integer id;

	@MapKeyType( @Type(type="integer") )
	@ElementCollection
	@Sort(type = SortType.NATURAL) 
	@Type(type = "float")
	@MapKeyColumn(nullable = false)
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
