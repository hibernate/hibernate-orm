/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.access;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class ClassAccessNoSettersEntity {
	@Id
	@Column(length = 4)
	private Integer code;

	@Column(length = 40)
	private String name;

	@SuppressWarnings("UnusedDeclaration")
	private ClassAccessNoSettersEntity() {

	}

	public ClassAccessNoSettersEntity(Integer code, String name) {
		this.code = code;
		this.name = name;
	}

	public Integer getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public static ClassAccessNoSettersEntity of(Integer code, String name) {
		return new ClassAccessNoSettersEntity( code, name );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ClassAccessNoSettersEntity that = (ClassAccessNoSettersEntity) o;
		return Objects.equals( code, that.code ) &&
				Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( code, name );
	}

	@Override
	public String toString() {
		return "ClassAccessNoSettersEntity{" +
				"code=" + code +
				", name='" + name + '\'' +
				'}';
	}
}
