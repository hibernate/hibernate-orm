/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;


/**
 * @author Wolfgang Voelkl
 */
public class MainObject {
	private Long id;
	private String description;
	private Object2 obj2;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Object2 getObj2() {
		return obj2;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String string) {
		description = string;
	}

	public void setObj2(Object2 object2) {
		this.obj2 = object2;
		if (object2 != null) {
			object2.setBelongsToMainObj(this);
		}
	}

}
