/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.ids;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class CompositeDateIdTestEntity {
	@EmbeddedId
	private DateEmbId id;

	@Audited
	private String str1;

	public CompositeDateIdTestEntity() {
	}

	public CompositeDateIdTestEntity(String str1) {
		this.str1 = str1;
	}

	public CompositeDateIdTestEntity(DateEmbId id, String str1) {
		this.id = id;
		this.str1 = str1;
	}

	public DateEmbId getId() {
		return id;
	}

	public void setId(DateEmbId id) {
		this.id = id;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CompositeDateIdTestEntity that = (CompositeDateIdTestEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str1 != null ? !str1.equals( that.str1 ) : that.str1 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (str1 != null ? str1.hashCode() : 0);
		return result;
	}
}
