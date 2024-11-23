/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.abstractembeddedcomponents.propertyref;



/**
 * @author Steve Ebersole
 */
public interface Server {
	public Long getId();
	public void setId(Long id);
	public String getServerType();
	public void setServerType(String serverType);
	public Address getAddress();
	public void setAddress(Address address);
}
