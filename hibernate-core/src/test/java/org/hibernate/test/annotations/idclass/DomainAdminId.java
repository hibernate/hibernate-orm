// $Id$
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.annotations.idclass;
import java.io.Serializable;

/**
 * A DomainAdminId.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
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
