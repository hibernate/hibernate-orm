/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package ee.estonia.envers.support.domain.naming;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * This class is used by {@link org.hibernate.envers.test.naming.EstonianTableAliasTest}.  It's necessary for this
 * class to exist in a package that begins with {@code ee}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class Parent implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@OneToMany
	private Set<Child> collection = new HashSet<Child>();

	public Parent() {
	}

	public Parent(String data) {
		this.data = data;
	}

	public Parent(String data, Long id) {
		this.id = id;
		this.data = data;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Set<Child> getCollection() {
		return collection;
	}

	public void setCollection(Set<Child> collection) {
		this.collection = collection;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Parent parent = (Parent) o;
		return Objects.equals( id, parent.id ) &&
				Objects.equals( data, parent.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "Parent{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
