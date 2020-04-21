/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.manytomany.defaults;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_city")
public class City {
	private Integer id;
	private String name;
	private Set<Item> stolenItems;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	@ManyToMany
	public Set<Item> getStolenItems() {
		return stolenItems;
	}

	public void setStolenItems(Set<Item> stolenItems) {
		this.stolenItems = stolenItems;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
