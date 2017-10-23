/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy;


/**
 * @author Emmanuel Bernard
 */
public class Customers implements java.io.Serializable {
	private int id;
	private String specified_column;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSpecified_column() {
		return specified_column;
	}

	public void setSpecified_column(String specified_column) {
		this.specified_column = specified_column;
	}
}
