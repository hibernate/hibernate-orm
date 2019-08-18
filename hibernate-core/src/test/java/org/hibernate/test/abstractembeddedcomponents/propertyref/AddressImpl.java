/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.abstractembeddedcomponents.propertyref;



/**
 * @author Steve Ebersole
 */
public class AddressImpl implements Address {
	private Long id;
	private String addressType;
	private Server server;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getAddressType() {
		return addressType;
	}

	@Override
	public void setAddressType(String addressType) {
		this.addressType = addressType;
	}

	@Override
	public Server getServer() {
		return server;
	}

	@Override
	public void setServer(Server server) {
		this.server = server;
	}
}
