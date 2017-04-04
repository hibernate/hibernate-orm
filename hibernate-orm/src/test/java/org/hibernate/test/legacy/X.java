/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: X.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.ArrayList;
import java.util.List;


public class X {

	private long id;
	private Y y;
	private List xxs = new ArrayList();

	/**
	 * Returns the id.
	 * @return long
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns the y.
	 * @return Y
	 */
	public Y getY() {
		return y;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Sets the y.
	 * @param y The y to set
	 */
	public void setY(Y y) {
		this.y = y;
	}
	
	public List getXxs() {
		return xxs;
	}

	public void setXxs(List xxs) {
		this.xxs = xxs;
	}

	public static class XX {
		private Long id;
		private X x;
		private XX() {}
		public XX(X x) {
			this.x = x;
		}
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public X getX() {
			return x;
		}

		public void setX(X x) {
			this.x = x;
		}

	}

}
