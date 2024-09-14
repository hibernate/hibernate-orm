/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;

public interface AbstractProxy extends FooProxy {
	void setAbstracts(java.util.Set arg0);
	java.util.Set getAbstracts();
	void setTime(java.sql.Time arg0);
	java.sql.Time getTime();
}
