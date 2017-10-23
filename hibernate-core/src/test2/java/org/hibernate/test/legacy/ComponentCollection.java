/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: ComponentCollection.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.util.List;

public class ComponentCollection implements Serializable {
	private String str;
	private List foos;
	private List floats;
	public List getFoos() {
		return foos;
	}

	public String getStr() {
		return str;
	}

	public void setFoos(List list) {
		foos = list;
	}

	public void setStr(String string) {
		str = string;
	}

	public List getFloats() {
		return floats;
	}

	public void setFloats(List list) {
		floats = list;
	}

}
