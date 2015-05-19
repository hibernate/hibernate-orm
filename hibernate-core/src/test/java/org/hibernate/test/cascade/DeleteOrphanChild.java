/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade;


/**
 * Created by IntelliJ IDEA.
 * User: Gail
 * Date: Jan 2, 2007
 * Time: 4:52:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeleteOrphanChild {
	private Long id;
	private Parent parent;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
