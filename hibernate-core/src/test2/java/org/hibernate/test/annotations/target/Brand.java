/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
