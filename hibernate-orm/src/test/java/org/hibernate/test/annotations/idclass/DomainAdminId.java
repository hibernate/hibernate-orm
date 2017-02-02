/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

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
