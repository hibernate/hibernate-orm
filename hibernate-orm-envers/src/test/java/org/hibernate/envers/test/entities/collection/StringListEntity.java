/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.collection;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.IndexColumn;
import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class StringListEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ElementCollection
	@IndexColumn(name = "list_index")
	private List<String> strings;

	public StringListEntity() {
		strings = new ArrayList<String>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<String> getStrings() {
		return strings;
	}

	public void setStrings(List<String> strings) {
		this.strings = strings;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof StringListEntity) ) {
			return false;
		}

		StringListEntity that = (StringListEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "SLE(id = " + id + ", strings = " + strings + ")";
	}
}