package org.hibernate.envers.test.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

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

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str != null ? !str.equals( that.str ) : that.str != null ) {
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
