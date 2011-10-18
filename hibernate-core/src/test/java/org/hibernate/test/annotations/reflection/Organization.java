//$Id$
package org.hibernate.test.annotations.reflection;


/**
 * @author Emmanuel Bernard
 */
public class Organization {
	private String organizationId;

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}
}
