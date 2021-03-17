/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.envers.boot.spi.ModifiedColumnNamingStrategy;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.mapping.Value;

import org.dom4j.Element;

/**
 * A {@link ModifiedColumnNamingStrategy} that adds modified columns with the following rules:
 * <ul>
 *     <li>If an audit annotation modified column name is supplied, use it directly with no suffix.</li>
 *     <li>If no audit annotation modified column name is present, use the property name appended with suffix.</li>
 * </ul>
 *
 * This is the default Envers modified column naming behavior.
 *
 * @author Chris Cranford
 * @since 5.4.7
 */
public class LegacyModifiedColumnNamingStrategy implements ModifiedColumnNamingStrategy {
	@Override
	public void addModifiedColumns(
			GlobalConfiguration globalCfg,
			Value value,
			Element parent,
			PropertyAuditingData propertyAuditingData) {
		final String columnName;
		if ( propertyAuditingData.isModifiedFlagNameExplicitlySpecified() ) {
			columnName = propertyAuditingData.getExplicitModifiedFlagName();
		}
		else {
			columnName = propertyAuditingData.getModifiedFlagName();
		}
		MetadataTools.addModifiedFlagPropertyWithColumn(
				parent,
				propertyAuditingData.getName(),
				globalCfg.getModifiedFlagSuffix(),
				propertyAuditingData.getModifiedFlagName(),
				columnName
		);
	}
}
