/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
