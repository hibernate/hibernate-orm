/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;


public class Z {

	private long id;
	private W w;

	/**
	 * 
	 */
	public Z() {
	}


	/**
	 * @return
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return
	 */
	public W getW() {
		return w;
	}

	/**
	 * @param l
	 */
	public void setId(long l) {
		id = l;
	}

	/**
	 * @param w
	 */
	public void setW(W w) {
		this.w = w;
	}

}
