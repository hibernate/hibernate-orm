/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.secondary.ids;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.SecondaryTable;

import org.hibernate.envers.Audited;
import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.orm.test.envers.entities.ids.MulId;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@SecondaryTable(name = "secondary")
@SecondaryAuditTable(secondaryTableName = "secondary", secondaryAuditTableName = "sec_mulid_versions")
@Audited
@IdClass(MulId.class)
public class SecondaryMulIdTestEntity {
	@Id
	private Integer id1;

	@Id
	private Integer id2;

	private String s1;

	@Column(table = "secondary")
	private String s2;

	public SecondaryMulIdTestEntity(MulId id, String s1, String s2) {
		this.id1 = id.getId1();
		this.id2 = id.getId2();
		this.s1 = s1;
		this.s2 = s2;
	}

	public SecondaryMulIdTestEntity(String s1, String s2) {
		this.s1 = s1;
		this.s2 = s2;
	}

	public SecondaryMulIdTestEntity() {
	}

	public Integer getId1() {
		return id1;
	}

	public void setId1(Integer id1) {
		this.id1 = id1;
	}

	public Integer getId2() {
		return id2;
	}

	public void setId2(Integer id2) {
		this.id2 = id2;
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
		if ( !(o instanceof SecondaryMulIdTestEntity) ) {
			return false;
		}

		SecondaryMulIdTestEntity that = (SecondaryMulIdTestEntity) o;

		if ( id1 != null ? !id1.equals( that.id1 ) : that.id1 != null ) {
			return false;
		}
		if ( id2 != null ? !id2.equals( that.id2 ) : that.id2 != null ) {
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
		result = (id1 != null ? id1.hashCode() : 0);
		result = 31 * result + (id2 != null ? id2.hashCode() : 0);
		result = 31 * result + (s1 != null ? s1.hashCode() : 0);
		result = 31 * result + (s2 != null ? s2.hashCode() : 0);
		return result;
	}
}
