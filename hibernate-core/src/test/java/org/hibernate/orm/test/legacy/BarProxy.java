/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;


public interface BarProxy extends AbstractProxy {
	void setBaz(Baz arg0);
	Baz getBaz();
	void setBarComponent(FooComponent arg0);
	FooComponent getBarComponent();
	//public void setBarString(String arg0);
	String getBarString();
	Object getObject();
	void setObject(Object o);
}
