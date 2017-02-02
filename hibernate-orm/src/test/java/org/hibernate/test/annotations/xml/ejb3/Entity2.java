/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.ejb3;
import java.util.List;

public class Entity2 {
	List field1;

	public List getField1() {
		return field1;
	}

	public void setField1(List field1) {
		this.field1 = field1;
	}
}
