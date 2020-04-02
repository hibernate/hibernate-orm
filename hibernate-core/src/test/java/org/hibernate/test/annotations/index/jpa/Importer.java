/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.index.jpa;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

/**
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
@Entity
public class Importer {
	@Id
	private long id;

	private String name;
	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable( name = "CAR_IMPORTER",indexes = @Index(columnList = "importers_id"))
	private List<Car> cars;

	public List<Car> getCars() {
		return cars;
	}

	public void setCars(List<Car> cars) {
		this.cars = cars;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
