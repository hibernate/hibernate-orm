/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.ids;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "MulId")
@IdClass(MulId.class)
public class MulIdTestEntity {
	@Id
	private Integer id1;

	@Id
	private Integer id2;

	@Audited
	private String str1;

	public MulIdTestEntity() {
	}

	public MulIdTestEntity(String str1) {
		this.str1 = str1;
	}

	public MulIdTestEntity(Integer id1, Integer id2, String str1) {
		this.id1 = id1;
		this.id2 = id2;
		this.str1 = str1;
	}

	public Integer getId1() {
		return id1;
	}

	public void setId1(Integer id1) {
		this.id1 = id1;
	}

	public Integer getId2() {
		return id2;
	}

	public void setId2(Integer id2) {
		this.id2 = id2;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MulIdTestEntity) ) {
			return false;
		}

		MulIdTestEntity that = (MulIdTestEntity) o;

		if ( id1 != null ? !id1.equals( that.id1 ) : that.id1 != null ) {
			return false;
		}
		if ( id2 != null ? !id2.equals( that.id2 ) : that.id2 != null ) {
			return false;
		}
		if ( str1 != null ? !str1.equals( that.str1 ) : that.str1 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id1 != null ? id1.hashCode() : 0);
		result = 31 * result + (id2 != null ? id2.hashCode() : 0);
		result = 31 * result + (str1 != null ? str1.hashCode() : 0);
		return result;
	}
}
