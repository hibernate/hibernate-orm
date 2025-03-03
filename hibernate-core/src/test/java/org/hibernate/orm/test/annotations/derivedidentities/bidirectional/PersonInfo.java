/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class PersonInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@OneToOne
	private Person id;

	@Basic
	private String info;

	public Person getId() {
		return this.id;
	}

	public void setId(Person id) {
		this.id = id;
	}

	public String getInfo() {
		return this.info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public int hashCode() {
		int hash = 0;
		hash += ( this.id != null ? this.id.hashCode() : 0 );
		return hash;
	}

	public boolean equals(Object object) {
		if ( !( object instanceof PersonInfo ) ) {
			return false;
		}
		PersonInfo other = (PersonInfo) object;

		return ( ( this.id != null ) || ( other.id == null ) ) && ( ( this.id == null ) || ( this.id.equals( other.id ) ) );
	}

	public String toString() {
		return "nogroup.hibertest.PersonInfo[ id=" + this.id + " ]";
	}
}
