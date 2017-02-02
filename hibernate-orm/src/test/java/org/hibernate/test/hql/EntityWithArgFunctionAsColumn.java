/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;


/**
 *
 * @author Gail Badner
 */
public class EntityWithArgFunctionAsColumn {
	private long id;
	private int lower;
	private String upper;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;		
	}

	public int getLower() {
		return lower;
	}
	public void setLower(int lower) {
		this.lower = lower;
	}

	public String getUpper() {
		return upper;
	}
	public void setUpper(String upper) {
		this.upper = upper;
	}
}
