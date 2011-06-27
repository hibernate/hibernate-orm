/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.target;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyJoinColumn;

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
