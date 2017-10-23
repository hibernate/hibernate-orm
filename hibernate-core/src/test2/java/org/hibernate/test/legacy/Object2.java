/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;


/**
 * 
 * @author Wolfgang Voelkl
 * 
 */
public class Object2 {
	private Long id;
	private String dummy;
	private MainObject belongsToMainObj;

	public Long getId() {
		return id;
	}

	public void setId(Long l) {
		this.id = l;
	}
	
	public String getDummy() {
		return dummy;
	}

	public void setDummy(String string) {
		dummy = string;
	}

	public MainObject getBelongsToMainObj() {
		return belongsToMainObj;
	}

	public void setBelongsToMainObj(MainObject object) {
		belongsToMainObj = object;
	}

}
