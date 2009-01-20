package org.hibernate.envers.configuration.metadata.reader;

/**
 * Implementations hold other audited properties.
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditedPropertiesHolder {
	/**
	 * Add an audited property.
	 * @param propertyName Name of the audited property.
	 * @param auditingData Data for the audited property.
	 */
	void addPropertyAuditingData(String propertyName, PropertyAuditingData auditingData);

	/**
	 * @param propertyName Name of a property.
	 * @return Auditing data for the property.
	 */
	PropertyAuditingData getPropertyAuditingData(String propertyName);
}
