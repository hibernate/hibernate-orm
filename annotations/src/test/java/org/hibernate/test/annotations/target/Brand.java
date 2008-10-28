//$Id$
package org.hibernate.test.annotations.target;

import java.util.Map;
import java.util.HashMap;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.MapKeyManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Brand {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToMany(targetEntity = LuggageImpl.class)
	@MapKey(targetElement = SizeImpl.class)
	private Map<Size, Luggage> luggagesBySize = new HashMap<Size, Luggage>();

	@CollectionOfElements(targetElement = SizeImpl.class)
	@MapKeyManyToMany(targetEntity = LuggageImpl.class)
	private Map<Luggage, Size> sizePerLuggage = new HashMap<Luggage, Size>();


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Map<Size, Luggage> getLuggagesBySize() {
		return luggagesBySize;
	}

	public void setLuggagesBySize(Map<Size, Luggage> luggagesBySize) {
		this.luggagesBySize = luggagesBySize;
	}

	public Map<Luggage, Size> getSizePerLuggage() {
		return sizePerLuggage;
	}

	public void setSizePerLuggage(Map<Luggage, Size> sizePerLuggage) {
		this.sizePerLuggage = sizePerLuggage;
	}
}
