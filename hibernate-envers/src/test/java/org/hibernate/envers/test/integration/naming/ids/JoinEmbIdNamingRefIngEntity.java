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
package org.hibernate.envers.test.integration.naming.ids;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * ReferencIng entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "JoinEmbIdRefIng")
public class JoinEmbIdNamingRefIngEntity {
	@Id
	@GeneratedValue
	private EmbIdNaming id;

	@Audited
	private String data;

	@Audited
	@ManyToOne
	@JoinColumns({
						 @JoinColumn(name = "XX_reference", referencedColumnName = "XX"),
						 @JoinColumn(name = "YY_reference", referencedColumnName = "YY")
				 })
	private JoinEmbIdNamingRefEdEntity reference;

	public JoinEmbIdNamingRefIngEntity() {
	}

	public JoinEmbIdNamingRefIngEntity(EmbIdNaming id, String data, JoinEmbIdNamingRefEdEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public JoinEmbIdNamingRefIngEntity(String data, JoinEmbIdNamingRefEdEntity reference) {
		this.data = data;
		this.reference = reference;
	}

	public EmbIdNaming getId() {
		return id;
	}

	public void setId(EmbIdNaming id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public JoinEmbIdNamingRefEdEntity getReference() {
		return reference;
	}

	public void setReference(JoinEmbIdNamingRefEdEntity reference) {
		this.reference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof JoinEmbIdNamingRefIngEntity) ) {
			return false;
		}

		JoinEmbIdNamingRefIngEntity that = (JoinEmbIdNamingRefIngEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "JoinEmbIdNamingRefIngEntity(id = " + id + ", data = " + data + ")";
	}
}