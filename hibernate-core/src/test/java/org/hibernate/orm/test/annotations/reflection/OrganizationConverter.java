/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.reflection;

import jakarta.persistence.AttributeConverter;

/**
 * Converts {@link Organization} <=> String
 */
public class OrganizationConverter implements AttributeConverter<Organization, String> {

	@Override
	public String convertToDatabaseColumn(Organization organization) {
		return "ORG-" + organization.getOrganizationId();
	}

	@Override
	public Organization convertToEntityAttribute(String organizationId) {
		Organization organization = new Organization();
		organization.setOrganizationId(organizationId.replace("ORG-", ""));
		return organization;
	}
}
