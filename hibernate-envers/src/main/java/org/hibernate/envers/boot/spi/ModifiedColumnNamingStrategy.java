/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.mapping.Value;

import org.dom4j.Element;

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
	 * @param globalCfg the envers global configuration
	 * @param value the property value
	 * @param parent the parent audited entity metamodel
	 * @param propertyAuditingData the property auditing data
	 */
	void addModifiedColumns(
			GlobalConfiguration globalCfg,
			Value value,
			Element parent,
			PropertyAuditingData propertyAuditingData);
}
