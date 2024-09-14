/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql;


/**
 * Implementation of HeresAnotherCrazyIdFieldName.
 *
 * @author Steve Ebersole
 */
public class HeresAnotherCrazyIdFieldName {
	private Long heresAnotherCrazyIdFieldName;
	private String name;

	public HeresAnotherCrazyIdFieldName() {
	}

	public HeresAnotherCrazyIdFieldName(String name) {
		this.name = name;
	}

	public Long getHeresAnotherCrazyIdFieldName() {
		return heresAnotherCrazyIdFieldName;
	}

	public void setHeresAnotherCrazyIdFieldName(Long heresAnotherCrazyIdFieldName) {
		this.heresAnotherCrazyIdFieldName = heresAnotherCrazyIdFieldName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
