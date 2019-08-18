/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Bar.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


public class Bar extends Abstract implements BarProxy, Named {
	private String barString;
	private FooComponent barComponent = new FooComponent("bar", 69, null, null);
	private Baz baz;
	private int x;
	private Object object;
	
	@Override
	public int getX() {
		return x;
	}
	@Override
	public void setX(int x) {
		this.x = x;
	}

	@Override
	public String getBarString() {
		return barString;
	}
	
	void setBarString(String barString) {
		this.barString = barString;
	}
	
	@Override
	public FooComponent getBarComponent() {
		return barComponent;
	}
	
	@Override
	public void setBarComponent(FooComponent barComponent) {
		this.barComponent = barComponent;
	}
	
	@Override
	public Baz getBaz() {
		return baz;
	}
	
	@Override
	public void setBaz(Baz baz) {
		this.baz = baz;
	}
	
	private String name = "bar";
	
	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Object getObject() {
		return object;
	}

	@Override
	public void setObject(Object object) {
		this.object = object;
	}

}







