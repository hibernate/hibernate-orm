/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.properties;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		UnversionedOptimisticLockingFieldEntity that = (UnversionedOptimisticLockingFieldEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( str, that.str );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, str );
	}
}
