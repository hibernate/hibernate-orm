/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.mapping.Value;

/**
 * Defines a naming strategy for applying modified columns to the audited entity metamodel.
 *
 * @author Chris Cranford
 * @since 5.4.7
 */
@Incubating
public interface ModifiedColumnNamingStrategy {
	/**
	 * Adds modified columns to the audited entity metamodel.
	 *
	 * @param configuration the envers configuration
	 * @param value the property value
	 * @param mapping the entity mapping model
	 * @param propertyAuditingData the property auditing data
	 */
	void addModifiedColumns(
			Configuration configuration,
			Value value,
			AttributeContainer mapping,
			PropertyAuditingData propertyAuditingData);
}
