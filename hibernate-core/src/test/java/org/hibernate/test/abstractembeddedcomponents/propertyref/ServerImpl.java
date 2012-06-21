package org.hibernate.test.abstractembeddedcomponents.propertyref;



/**
 * @author Steve Ebersole
 */
public class ServerImpl implements Server {
	private Long id;
	private String serverType;
	private Address address;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getServerType() {
		return serverType;
	}

	public void setServerType(String serverType) {
		this.serverType = serverType;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}
