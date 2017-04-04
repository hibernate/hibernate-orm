/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;


public interface AbstractProxy extends FooProxy {
	public void setAbstracts(java.util.Set arg0);
	public java.util.Set getAbstracts();
	public void setTime(java.sql.Time arg0);
	public java.sql.Time getTime();
}





