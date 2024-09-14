/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
interface TargetColumnAdapter {
	void setName(String value);

	void setTable(String value);

	void setNullable(Boolean value);

	void setUnique(Boolean value);

	void setColumnDefinition(String value);

	void setLength(Integer value);

	void setPrecision(Integer value);

	void setScale(Integer value);

	void setDefault(String value);

	void setCheck(String value);

	void setComment(String value);

	void setRead(String value);

	void setWrite(String value);

	void setInsertable(Boolean value);

	void setUpdatable(Boolean value);
}
