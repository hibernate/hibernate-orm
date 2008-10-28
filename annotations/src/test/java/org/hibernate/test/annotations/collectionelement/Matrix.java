//$
package org.hibernate.test.annotations.collectionelement;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Type;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Matrix {
	@Id
	@GeneratedValue
	private Integer id;
	
	@MapKey(type = @Type(type="integer") )
	@CollectionOfElements
	@Sort(type = SortType.NATURAL) 
	@Type(type = "float")
	private SortedMap<Integer, Float> values = new TreeMap<Integer, Float>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<Integer, Float> getValues() {
		return values;
	}

	public void setValues(SortedMap<Integer, Float> values) {
		this.values = values;
	}
}
