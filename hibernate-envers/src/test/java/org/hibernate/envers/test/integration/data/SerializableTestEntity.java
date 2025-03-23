/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SerializableTestEntity) ) {
			return false;
		}

		SerializableTestEntity that = (SerializableTestEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( obj != null ? !obj.equals( that.obj ) : that.obj != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (obj != null ? obj.hashCode() : 0);
		return result;
	}
}
