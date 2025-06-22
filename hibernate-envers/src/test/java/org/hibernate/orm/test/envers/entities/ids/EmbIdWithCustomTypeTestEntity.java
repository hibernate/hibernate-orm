/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.ids;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Slawek Garwol (slawekgarwol at gmail dot com)
 */
@Entity
@Table(name = "EmbIdWithCustType")
public class EmbIdWithCustomTypeTestEntity {
	@EmbeddedId
	private EmbIdWithCustomType id;

	@Audited
	private String str1;

	public EmbIdWithCustomTypeTestEntity() {
	}

	public EmbIdWithCustomTypeTestEntity(EmbIdWithCustomType id, String str1) {
		this.id = id;
		this.str1 = str1;
	}

	public EmbIdWithCustomType getId() {
		return id;
	}

	public void setId(EmbIdWithCustomType id) {
		this.id = id;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof EmbIdWithCustomTypeTestEntity) ) {
			return false;
		}

		EmbIdWithCustomTypeTestEntity that = (EmbIdWithCustomTypeTestEntity) obj;
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
		return result;
	}
}
