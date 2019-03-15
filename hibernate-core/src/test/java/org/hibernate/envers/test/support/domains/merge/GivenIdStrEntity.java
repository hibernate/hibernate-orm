/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.merge;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
@AuditTable("GIVENIDSTRENTITY_AUD")
public class GivenIdStrEntity {
	@Id
	private Integer id;

	private String data;

	public GivenIdStrEntity() {
	}

	public GivenIdStrEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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
		GivenIdStrEntity that = (GivenIdStrEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "GivenIdStrEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
