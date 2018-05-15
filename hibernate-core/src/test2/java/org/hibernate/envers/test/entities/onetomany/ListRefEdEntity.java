/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.onetomany;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * ReferencEd entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class ListRefEdEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToMany(mappedBy = "reference")
	private List<ListRefIngEntity> reffering;

	public ListRefEdEntity() {
	}

	public ListRefEdEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public ListRefEdEntity(String data) {
		this.data = data;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public List<ListRefIngEntity> getReffering() {
		return reffering;
	}

	public void setReffering(List<ListRefIngEntity> reffering) {
		this.reffering = reffering;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ListRefEdEntity) ) {
			return false;
		}

		ListRefEdEntity that = (ListRefEdEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ListRefEdEntity(id = " + id + ", data = " + data + ")";
	}
}