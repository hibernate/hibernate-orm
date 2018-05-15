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
public class EntityWithNoArgFunctionAsColumn {
	private long id;
	private String user;
	private String currentDate;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}

	public String getCurrentDate() {
		return currentDate;
	}
	public void setCurrentDate(String currentDate) {
		this.currentDate = currentDate;
	}
}
