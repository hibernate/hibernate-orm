/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;
import java.io.Serializable;

/**
 * @author Stefano Travelli
 */
public class OuterKey implements Serializable {
	private Middle root;
	private String detailId;

	public Middle getRoot() {
		return root;
	}

	public void setRoot(Middle root) {
		this.root = root;
	}

	public String getDetailId() {
		return detailId;
	}

	public void setDetailId(String detailId) {
		this.detailId = detailId;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OuterKey)) return false;

		final OuterKey cidDetailID = (OuterKey) o;

		if (detailId != null ? !detailId.equals(cidDetailID.detailId) : cidDetailID.detailId != null) return false;
		if ( root != null ? !root.equals(cidDetailID.root ) : cidDetailID.root != null) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = ( root != null ? root.hashCode() : 0);
		result = 29 * result + (detailId != null ? detailId.hashCode() : 0);
		return result;
	}
}
