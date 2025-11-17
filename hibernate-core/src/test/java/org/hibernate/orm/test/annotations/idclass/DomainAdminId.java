/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclass;
import java.io.Serializable;

/**
 * A DomainAdminId.
 *
 * @author Stale W. Pedersen
 */
@SuppressWarnings("serial")
public class DomainAdminId implements Serializable {

	private String domainName;

	private String adminUser;

	public DomainAdminId() {
	}

	public DomainAdminId(String domainName, String adminUser) {
		this.domainName = domainName;
		this.adminUser = adminUser;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getAdminUser() {
		return adminUser;
	}

	public void setAdminUser(String adminUser) {
		this.adminUser = adminUser;
	}

	@Override
	public boolean equals(Object o) {
		return ( ( o instanceof DomainAdminId ) && domainName.equals( ( ( DomainAdminId ) o ).getDomainName() ) &&
				adminUser.equals( ( ( DomainAdminId ) o ).getAdminUser() ) );
	}

	@Override
	public int hashCode() {
		return ( domainName + adminUser ).hashCode();
	}
}
