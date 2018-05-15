/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Nameable.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


/**
 *
 */
public interface Nameable {
	public String getName();
	public void setName(String name);
	public Long getKey();
	public void setKey(Long key);
}






