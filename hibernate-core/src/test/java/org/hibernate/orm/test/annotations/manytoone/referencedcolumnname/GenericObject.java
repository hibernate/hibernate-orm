/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone.referencedcolumnname;
import java.io.Serializable;
import java.rmi.server.UID;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@MappedSuperclass
public class GenericObject implements Serializable {
	protected int id;
	protected int version;
	protected UID uid = new UID();

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Version
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void incrementVersion() {
		this.version++;
	}

	public boolean equals(Object other) {
		if ( this == other )
			return true;
		if ( ( other == null ) || !( other.getClass().equals( this.getClass() ) ) )
			return false;
		GenericObject anObject = (GenericObject) other;
		if ( this.id == 0 || anObject.id == 0 )
			return false;

		return ( this.id == anObject.id );
	}

	public int hashCode() {
		if ( this.id == 0 )
			return super.hashCode();
		return this.id;
	}

	@Transient
	public UID getUid() {
		return uid;
	}

	public void setUid(UID uid) {
		this.uid = uid;
	}
}
