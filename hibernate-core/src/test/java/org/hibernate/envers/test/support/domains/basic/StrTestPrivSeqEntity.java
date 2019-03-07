/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.Objects;

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
	@SequenceGenerator(name = "StrTestPrivSeq", sequenceName = "STRTESTPRIV_SEQ", allocationSize = 1, initialValue = 1)
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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StrTestPrivSeqEntity that = (StrTestPrivSeqEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( str, that.str );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, str );
	}

	@Override
	public String toString() {
		return "StrTestPrivSeqEntity{" +
				"id=" + id +
				", str='" + str + '\'' +
				'}';
	}
}
