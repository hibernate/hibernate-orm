/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class Person {
	private Long id;

	private String name;

	private Integer weight;

	private Integer height;

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Person ) ) return false;

		Person person = (Person) o;

		if ( height != null ? !height.equals( person.height ) : person.height != null ) return false;
		if ( id != null ? !id.equals( person.id ) : person.id != null ) return false;
		if ( name != null ? !name.equals( person.name ) : person.name != null ) return false;
		if ( weight != null ? !weight.equals( person.weight ) : person.weight != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		result = 31 * result + ( weight != null ? weight.hashCode() : 0 );
		result = 31 * result + ( height != null ? height.hashCode() : 0 );
		return result;
	}

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

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}
}
