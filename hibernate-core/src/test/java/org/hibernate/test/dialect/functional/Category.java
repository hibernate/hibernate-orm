package org.hibernate.test.dialect.functional;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class Category implements Serializable {
	@Id
	public Integer id;

	public String name;

	@OneToMany(mappedBy = "category")
	public List<Product2> products;

	public Category() {
	}

	public Category(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( ! ( o instanceof Category ) ) return false;

		Category category = (Category) o;

		if ( id != null ? !id.equals( category.id ) : category.id != null ) return false;
		if ( name != null ? !name.equals( category.name ) : category.name != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Category(id = " + id + ", name = " + name + ")";
	}
}
