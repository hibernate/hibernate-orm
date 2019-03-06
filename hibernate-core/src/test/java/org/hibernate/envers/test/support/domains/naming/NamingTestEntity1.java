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
import javax.persistence.Table;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "naming_test_entity_1")
@AuditTable("naming_test_entity_1_versions")
public class NamingTestEntity1 {
	@Id
	@GeneratedValue
	@Column(name = "nte_id")
	private Integer id;

	@Column(name = "nte_data")
	@Audited
	private String data;

	public NamingTestEntity1() {
	}

	public NamingTestEntity1(String data) {
		this.data = data;
	}

	public NamingTestEntity1(Integer id, String data) {
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
		NamingTestEntity1 that = (NamingTestEntity1) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}
}
