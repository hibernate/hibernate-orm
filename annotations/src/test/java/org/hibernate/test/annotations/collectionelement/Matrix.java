//$
package org.hibernate.test.annotations.collectionelement;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
	@ElementCollection
	@Sort(type = SortType.NATURAL) 
	@Type(type = "float")
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
