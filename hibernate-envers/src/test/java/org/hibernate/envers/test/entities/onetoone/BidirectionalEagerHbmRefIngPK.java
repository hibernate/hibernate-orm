/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.test.entities.onetoone;

import org.hibernate.envers.Audited;

@Audited
public class BidirectionalEagerHbmRefIngPK {
	private long id;
	private String data;
	private BidirectionalEagerHbmRefEdPK reference;

	public BidirectionalEagerHbmRefIngPK() {
	}

	public BidirectionalEagerHbmRefIngPK(String data) {
		this.data = data;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public BidirectionalEagerHbmRefEdPK getReference() {
		return reference;
	}

	public void setReference(BidirectionalEagerHbmRefEdPK reference) {
		this.reference = reference;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof BidirectionalEagerHbmRefIngPK) ) {
			return false;
		}

		BidirectionalEagerHbmRefIngPK that = (BidirectionalEagerHbmRefIngPK) o;

		if ( id != that.id ) {
			return false;
		}
		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "BidirectionalEagerHbmRefIngPK(id = " + id + ", data = " + data + ")";
	}
}
