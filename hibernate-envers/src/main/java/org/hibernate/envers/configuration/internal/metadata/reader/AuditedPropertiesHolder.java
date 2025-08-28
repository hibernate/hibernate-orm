/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.List;

/**
 * Implementations hold other audited properties.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 * @author Chris Cranford
 */
public interface AuditedPropertiesHolder {
	/**
	 * Add an audited property.
	 *
	 * @param propertyName Name of the audited property.
	 * @param auditingData Data for the audited property.
	 */
	void addPropertyAuditingData(String propertyName, PropertyAuditingData auditingData);

	/**
	 * @param propertyName Name of a property.
	 *
	 * @return Auditing data for the property.
	 */
	PropertyAuditingData getPropertyAuditingData(String propertyName);

	/**
	 * @return true if the holder contains any audited property
	 */
	boolean isEmpty();

	/**
	 * @return true if the holder contains the given audited property
	 */
	boolean contains(String propertyName);

	/**
	 * @return the holder's property audit overrides
	 */
	List<AuditOverrideData> getAuditingOverrides();
}
