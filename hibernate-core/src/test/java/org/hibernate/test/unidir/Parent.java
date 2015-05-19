/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Parent.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.unidir;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class Parent {
	private String name;
	private long ssn;
	private List children = new ArrayList();
	Parent() {}
	public Parent(String name, long ssn) {
		this.name = name;
		this.ssn = ssn;
	}
	public List getChildren() {
		return children;
	}
	public void setChildren(List children) {
		this.children = children;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getSsn() {
		return ssn;
	}
	public void setSsn(long ssn) {
		this.ssn = ssn;
	}
}
