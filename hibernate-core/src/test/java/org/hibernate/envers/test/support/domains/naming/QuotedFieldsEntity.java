/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.naming;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class QuotedFieldsEntity {
	@Id
	@GeneratedValue
	@Column(name = "`id`")
	private Long id;

	@Column(name = "`data1`")
	@Audited
	private String data1;

	@Column(name = "`data2`")
	@Audited
	private Integer data2;

	public QuotedFieldsEntity() {
	}

	public QuotedFieldsEntity(String data1, Integer data2) {
		this.data1 = data1;
		this.data2 = data2;
	}

	public QuotedFieldsEntity(Long id, String data1, Integer data2) {
		this.id = id;
		this.data1 = data1;
		this.data2 = data2;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData1() {
		return data1;
	}

	public void setData1(String data1) {
		this.data1 = data1;
	}

	public Integer getData2() {
		return data2;
	}

	public void setData2(Integer data2) {
		this.data2 = data2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		QuotedFieldsEntity that = (QuotedFieldsEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data1, that.data1 ) &&
				Objects.equals( data2, that.data2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data1, data2 );
	}

	@Override
	public String toString() {
		return "QuotedFieldsEntity{" +
				"id=" + id +
				", data1='" + data1 + '\'' +
				", data2=" + data2 +
				'}';
	}
}
