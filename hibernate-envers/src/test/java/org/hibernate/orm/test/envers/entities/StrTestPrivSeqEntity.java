/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Duplicate of {@link StrTestEntity} but with private sequence generator.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "STRSEQ")
public class StrTestPrivSeqEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "StrTestPrivSeq")
	@SequenceGenerator(name = "StrTestPrivSeq", sequenceName = "STRTESTPRIV_SEQ",
					allocationSize = 1, initialValue = 1)
	private Integer id;

	@Audited
	private String str;

	public StrTestPrivSeqEntity() {
	}

	public StrTestPrivSeqEntity(String str, Integer id) {
		this.str = str;
		this.id = id;
	}

	public StrTestPrivSeqEntity(String str) {
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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof StrTestPrivSeqEntity) ) {
			return false;
		}

		StrTestPrivSeqEntity that = (StrTestPrivSeqEntity) o;

		if ( id != null ? !id.equals( that.getId() ) : that.getId() != null ) {
			return false;
		}
		if ( str != null ? !str.equals( that.getStr() ) : that.getStr() != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str != null ? str.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "STPSE(id = " + id + ", str = " + str + ")";
	}
}
