/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.target;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Brand {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToMany(targetEntity = LuggageImpl.class)
	@MapKeyClass(SizeImpl.class)
	private Map<Size, Luggage> luggagesBySize = new HashMap<Size, Luggage>();

	@ElementCollection(targetClass = SizeImpl.class)
	@MapKeyClass(LuggageImpl.class)
	@MapKeyJoinColumn
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
