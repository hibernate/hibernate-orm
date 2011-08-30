package org.hibernate.test.abstractembeddedcomponents.propertyref;


/**
 * @author Steve Ebersole
 */
public interface Address {
	public Long getId();
	public void setId(Long id);
	public String getAddressType();
	public void setAddressType(String addressType);
	public Server getServer();
	public void setServer(Server server);
}
