/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.reflection;

import javax.persistence.AttributeConverter;

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
