/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.abstractembeddedcomponents.propertyref;


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
