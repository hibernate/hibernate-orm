/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SerializableTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private SerObject obj;

	public SerializableTestEntity() {
	}

	public SerializableTestEntity(SerObject obj) {
		this.obj = obj;
	}

	public SerializableTestEntity(Integer id, SerObject obj) {
		this.obj = obj;
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public SerObject getObj() {
		return obj;
	}

	public void setObj(SerObject obj) {
		this.obj = obj;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		SerializableTestEntity that = (SerializableTestEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( obj, that.obj );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, obj );
	}
}