/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Product.java 5725 2005-02-14 12:10:15Z oneovthafew $
package org.hibernate.test.orphan;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Product implements Serializable {
	private String name;
	private Set parts = new HashSet();
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set getParts() {
		return parts;
	}
	public void setParts(Set parts) {
		this.parts = parts;
	}
}
