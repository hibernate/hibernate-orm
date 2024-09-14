/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;

public class Bar extends Abstract implements BarProxy, Named {
	private String barString;
	private FooComponent barComponent = new FooComponent("bar", 69, null, null);
	private Baz baz;
	private int x;
	private Object object;

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public String getBarString() {
		return barString;
	}

	void setBarString(String barString) {
		this.barString = barString;
	}

	public FooComponent getBarComponent() {
		return barComponent;
	}

	public void setBarComponent(FooComponent barComponent) {
		this.barComponent = barComponent;
	}

	public Baz getBaz() {
		return baz;
	}

	public void setBaz(Baz baz) {
		this.baz = baz;
	}

	private String name = "bar";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

}
