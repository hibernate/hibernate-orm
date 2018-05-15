/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

/**
 * Entity used to test {@code NULL} values ordering in SQL {@code ORDER BY} clause.
 * Implementation note: By default H2 places {@code NULL} values first.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class Zoo implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@OneToMany
	@JoinColumn(name = "zoo_id")
	@org.hibernate.annotations.OrderBy(clause = "name asc nulls last") // By default H2 places NULL values first.
	private Set<Tiger> tigers = new HashSet<Tiger>();

	@OneToMany
	@JoinColumn(name = "zoo_id")
	@javax.persistence.OrderBy("name asc nulls last") // According to JPA specification this is illegal, but works in Hibernate.
	private Set<Monkey> monkeys = new HashSet<Monkey>();

	@OneToMany
	@JoinColumn(name = "zoo_id")
	@javax.persistence.OrderBy("lastName desc nulls last, firstName asc nulls LaSt") // Sorting by multiple columns.
	private Set<Visitor> visitors = new HashSet<Visitor>();

	public Zoo() {
	}

	public Zoo(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( ! ( o instanceof Zoo ) ) return false;

		Zoo zoo = (Zoo) o;

		if ( id != null ? !id.equals( zoo.id ) : zoo.id != null ) return false;
		if ( name != null ? !name.equals( zoo.name ) : zoo.name != null ) return false;

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
		return "Zoo(id = " + id + ", name = " + name + ")";
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

	public Set<Tiger> getTigers() {
		return tigers;
	}

	public void setTigers(Set<Tiger> tigers) {
		this.tigers = tigers;
	}

	public Set<Monkey> getMonkeys() {
		return monkeys;
	}

	public void setMonkeys(Set<Monkey> monkeys) {
		this.monkeys = monkeys;
	}

	public Set<Visitor> getVisitors() {
		return visitors;
	}

	public void setVisitors(Set<Visitor> visitors) {
		this.visitors = visitors;
	}
}
