/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Fixed.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Fixed extends Broken {
	private Set set;
	private List list = new ArrayList();

	public Set getSet() {
		return set;
	}

	public void setSet(Set set) {
		this.set = set;
	}

	public List getList() {
		return list;
	}

	public void setList(List list) {
		this.list = list;
	}

}
