/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gail Badner
 */
public class Category {
	private Long id;
	private String name;
	private Item exampleItem;
	private int version;
	private Set<SubCategory> subCategories = new HashSet<SubCategory>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Item getExampleItem() {
		return exampleItem;
	}

	public void setExampleItem(Item exampleItem) {
		this.exampleItem = exampleItem;
	}

	public Set<SubCategory> getSubCategories() {
		return subCategories;
	}

	public void setSubCategories(Set<SubCategory> subCategories) {
		this.subCategories = subCategories;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "Category{" +
				"id=" + id +
				", name='" + name + '\'' +
				", version=" + version +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Category category = (Category) o;

		if ( name != null ? !name.equals( category.name ) : category.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}
}
