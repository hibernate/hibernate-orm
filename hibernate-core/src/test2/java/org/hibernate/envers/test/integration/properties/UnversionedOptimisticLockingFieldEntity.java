/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.properties;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.envers.Audited;

/**
 * @author Nicolas Doroskevich
 */
@Audited
@Table(name = "UnverOptimLockField")
@Entity
public class UnversionedOptimisticLockingFieldEntity {

	@Id
	@GeneratedValue
	private Integer id;

	private String str;

	@Version
	private int optLocking;

	public UnversionedOptimisticLockingFieldEntity() {
	}

	public UnversionedOptimisticLockingFieldEntity(String str) {
		this.str = str;
	}

	public UnversionedOptimisticLockingFieldEntity(Integer id, String str) {
		this.id = id;
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

	public int getOptLocking() {
		return optLocking;
	}

	public void setOptLocking(int optLocking) {
		this.optLocking = optLocking;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof UnversionedOptimisticLockingFieldEntity) ) {
			return false;
		}

		UnversionedOptimisticLockingFieldEntity that = (UnversionedOptimisticLockingFieldEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str != null ? !str.equals( that.str ) : that.str != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str != null ? str.hashCode() : 0);
		return result;
	}

}
