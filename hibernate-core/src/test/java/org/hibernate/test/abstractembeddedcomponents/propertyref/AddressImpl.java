package org.hibernate.test.abstractembeddedcomponents.propertyref;



/**
 * @author Steve Ebersole
 */
public class AddressImpl implements Address {
	private Long id;
	private String addressType;
	private Server server;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAddressType() {
		return addressType;
	}

	public void setAddressType(String addressType) {
		this.addressType = addressType;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}
}
