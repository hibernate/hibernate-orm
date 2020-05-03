/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.manytomany.defaults;

import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/**
 * @author Gail Badner
 */
@Entity(name="ITEM")
public class Item {
	private Integer id;
	private Set<City> producedInCities;

	@Id
	@GeneratedValue
	@Column(name="iId")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToMany
	public Set<City> getProducedInCities() {
		return producedInCities;
	}

	public void setProducedInCities(Set<City> producedInCities) {
		this.producedInCities = producedInCities;
	}
}
