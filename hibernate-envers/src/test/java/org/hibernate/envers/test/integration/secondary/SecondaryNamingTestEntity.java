/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.secondary;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;

import org.hibernate.envers.Audited;
import org.hibernate.envers.SecondaryAuditTable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@SecondaryTable(name = "secondary")
@SecondaryAuditTable(secondaryTableName = "secondary", secondaryAuditTableName = "sec_versions")
@Audited
public class SecondaryNamingTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String s1;

	@Column(table = "secondary")
	private String s2;

	public SecondaryNamingTestEntity(Integer id, String s1, String s2) {
		this.id = id;
		this.s1 = s1;
		this.s2 = s2;
	}

	public SecondaryNamingTestEntity(String s1, String s2) {
		this.s1 = s1;
		this.s2 = s2;
	}

	public SecondaryNamingTestEntity() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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
		if ( !(o instanceof SecondaryNamingTestEntity) ) {
			return false;
		}

		SecondaryNamingTestEntity that = (SecondaryNamingTestEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
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
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (s1 != null ? s1.hashCode() : 0);
		result = 31 * result + (s2 != null ? s2.hashCode() : 0);
		return result;
	}
}
