/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package ee.estonia.envers.support.domain.naming;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * This class is used by {@link org.hibernate.envers.test.naming.EstonianTableAliasTest}.  It's necessary for this
 * class to exist in a package that begins with {@code ee}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class Child implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	public Child() {
	}

	public Child(String data) {
		this.data = data;
	}

	public Child(String data, Long id) {
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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Child child = (Child) o;
		return Objects.equals( id, child.id ) &&
				Objects.equals( data, child.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "Child{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}