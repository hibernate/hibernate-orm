/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package ee.estonia.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Child) ) {
			return false;
		}

		Child that = (Child) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Child(id = " + id + ", data = " + data + ")";
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
}