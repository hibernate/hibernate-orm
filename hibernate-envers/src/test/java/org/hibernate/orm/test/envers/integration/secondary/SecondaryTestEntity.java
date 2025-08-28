/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@SecondaryTable(name = "secondary")
@Audited
public class SecondaryTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String s1;

	@Column(table = "secondary")
	private String s2;

	public SecondaryTestEntity(Integer id, String s1, String s2) {
		this.id = id;
		this.s1 = s1;
		this.s2 = s2;
	}

	public SecondaryTestEntity(String s1, String s2) {
		this.s1 = s1;
		this.s2 = s2;
	}

	public SecondaryTestEntity() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getS1() {
		return s1;
	}

	public void setS1(String s1) {
		this.s1 = s1;
	}

	public String getS2() {
		return s2;
	}

	public void setS2(String s2) {
		this.s2 = s2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SecondaryTestEntity) ) {
			return false;
		}

		SecondaryTestEntity that = (SecondaryTestEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( s1 != null ? !s1.equals( that.s1 ) : that.s1 != null ) {
			return false;
		}
		if ( s2 != null ? !s2.equals( that.s2 ) : that.s2 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (s1 != null ? s1.hashCode() : 0);
		result = 31 * result + (s2 != null ? s2.hashCode() : 0);
		return result;
	}
}
