/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Abstract.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.Set;

public abstract class Abstract extends Foo implements AbstractProxy {
	
	private java.sql.Time time;
	private Set abstracts;
	
	@Override
	public java.sql.Time getTime() {
		return time;
	}
	
	@Override
	public void setTime(java.sql.Time time) {
		this.time = time;
	}
	
	@Override
	public Set getAbstracts() {
		return abstracts;
	}
	
	@Override
	public void setAbstracts(Set abstracts) {
		this.abstracts = abstracts;
	}
	
}






