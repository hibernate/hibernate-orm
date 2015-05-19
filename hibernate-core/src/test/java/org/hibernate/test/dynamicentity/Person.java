/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dynamicentity;
import java.util.Set;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface Person {
	public Long getId();
	public void setId(Long id);
	public String getName();
	public void setName(String name);
	public Address getAddress();
	public void setAddress(Address address);
	public Set getFamily();
	public void setFamily(Set family);
}
