/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class BasicTestEntity1 {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String str1;

	@Audited
	private long long1;

	public BasicTestEntity1() {
	}

	public BasicTestEntity1(String str1, long long1) {
		this.str1 = str1;
		this.long1 = long1;
	}

	public BasicTestEntity1(Integer id, String str1, long long1) {
		this.id = id;
		this.str1 = str1;
		this.long1 = long1;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public long getLong1() {
		return long1;
	}

	public void setLong1(long long1) {
		this.long1 = long1;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof BasicTestEntity1) ) {
			return false;
		}

		BasicTestEntity1 that = (BasicTestEntity1) o;

		if ( long1 != that.long1 ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str1 != null ? !str1.equals( that.str1 ) : that.str1 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str1 != null ? str1.hashCode() : 0);
		result = 31 * result + (int) (long1 ^ (long1 >>> 32));
		return result;
	}
}
