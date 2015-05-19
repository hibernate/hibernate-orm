/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

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

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( number != null ? !number.equals( that.number ) : that.number != null ) {
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
