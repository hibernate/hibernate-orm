/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dynamicentity;


/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface Address {
	public Long getId();
	public void setId(Long id);

	public String getStreet();
	public void setStreet(String street);

	public String getCity();
	public void setCity(String city);

	public String getPostalCode();
	public void setPostalCode(String postalCode);
}
