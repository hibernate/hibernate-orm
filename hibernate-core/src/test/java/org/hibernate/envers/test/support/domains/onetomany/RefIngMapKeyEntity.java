/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.onetomany;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class RefIngMapKeyEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ManyToOne
	private RefEdMapKeyEntity reference;

	@Audited
	private String data;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public RefEdMapKeyEntity getReference() {
		return reference;
	}

	public void setReference(RefEdMapKeyEntity reference) {
		this.reference = reference;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		RefIngMapKeyEntity that = (RefIngMapKeyEntity) o;
		return Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return "RefIngMapKeyEntity{" +
				"id=" + id +
				", reference=" + reference +
				", data='" + data + '\'' +
				'}';
	}
}