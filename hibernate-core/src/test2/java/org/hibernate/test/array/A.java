/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.array;


/**
 * @author Emmanuel Bernard
 */
public class A {
	private Integer id;
	private B[] bs;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public B[] getBs() {
		return bs;
	}

	public void setBs(B[] bs) {
		this.bs = bs;
	}
}
