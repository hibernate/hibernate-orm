/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.envers.Audited;

/**
 * Duplicate of {@link IntTestEntity} but with private sequence generator.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class IntTestPrivSeqEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IntTestPrivSeq")
	@SequenceGenerator(name = "IntTestPrivSeq", sequenceName = "INTTESTPRIV_SEQ",
					allocationSize = 1, initialValue = 1)
	private Integer id;

	@Audited
	@Column(name = "NUMERIC_VALUE")
	private Integer number;

	public IntTestPrivSeqEntity() {
	}

	public IntTestPrivSeqEntity(Integer number, Integer id) {
		this.id = id;
		this.number = number;
	}

	public IntTestPrivSeqEntity(Integer number) {
		this.number = number;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof IntTestPrivSeqEntity) ) {
			return false;
		}

		IntTestPrivSeqEntity that = (IntTestPrivSeqEntity) o;

		if ( id != null ? !id.equals( that.getId() ) : that.getId() != null ) {
			return false;
		}
		if ( number != null ? !number.equals( that.getNumber() ) : that.getNumber() != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (number != null ? number.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ITPSE(id = " + id + ", number = " + number + ")";
	}
}
