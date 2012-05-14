package org.hibernate.ejb.test.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public class TestCurrentTenantIdentifierResolver implements
		CurrentTenantIdentifierResolver {
	private String currentTenantIdentifier;

	@Override
	public boolean validateExistingCurrentSessions() {
		return false;
	}

	@Override
	public String resolveCurrentTenantIdentifier() {
		return currentTenantIdentifier;
	}

	public void setCurrentTenantIdentifier(String tenantId) {
		this.currentTenantIdentifier = tenantId;
	}
}