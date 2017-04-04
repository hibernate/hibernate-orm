/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: BarProxy.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


public interface BarProxy extends AbstractProxy {
	public void setBaz(Baz arg0);
	public Baz getBaz();
	public void setBarComponent(FooComponent arg0);
	public FooComponent getBarComponent();
	//public void setBarString(String arg0);
	public String getBarString();
	public Object getObject();
	public void setObject(Object o);
}





