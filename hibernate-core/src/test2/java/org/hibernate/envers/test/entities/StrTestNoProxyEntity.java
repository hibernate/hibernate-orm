/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Proxy;
import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "STR_TEST_NP")
@Proxy(lazy = false)
public class StrTestNoProxyEntity implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String str;

	public StrTestNoProxyEntity() {
	}

	public StrTestNoProxyEntity(String str, Integer id) {
		this.str = str;
		this.id = id;
	}

	public StrTestNoProxyEntity(String str) {
		this.str = str;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof StrTestNoProxyEntity) ) {
			return false;
		}

		StrTestNoProxyEntity that = (StrTestNoProxyEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str != null ? !str.equals( that.str ) : that.str != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str != null ? str.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "STNPE(id = " + id + ", str = " + str + ")";
	}
}
