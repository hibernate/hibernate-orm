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
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.hibernate.type.BasicType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;

import org.dom4j.Element;

/**
 * A {@link ModifiedColumnNamingStrategy} that adds modified columns with the following rules:
 * <ul>
 *     <li>For basic types, prioritizes audit annotation naming followed by physical column name appended with suffix.</li>
 *     <li>For associations with single column foreign keys, behaves like basic types.</li>
 *     <li>For associations with multiple column foreign keys, prioritizes audit annotation naming followed by using property name.</li>
 *     <li>For embeddables, behaves like associations with multiple column foreign keys</li>
 * </ul>
 *
 * @author Chris Cranford
 * @since 5.4.7
 */
public class ImprovedModifiedColumnNamingStrategy extends LegacyModifiedColumnNamingStrategy {
	@Override
	public void addModifiedColumns(
			GlobalConfiguration globalCfg,
			Value value,
			Element parent,
			PropertyAuditingData propertyAuditingData) {

		boolean basicType = value.getType() instanceof BasicType;
		boolean toOneType = value.getType() instanceof ManyToOneType || value.getType() instanceof OneToOneType;

		if ( basicType || toOneType ) {
			if ( value.getColumnSpan() == 1 ) {
				Selectable selectable = value.getColumnIterator().next();
				if ( selectable instanceof Column ) {
					// This should not be applied for formulas
					final String columnName;
					if ( !propertyAuditingData.isModifiedFlagNameExplicitlySpecified() ) {
						columnName = ( (Column) selectable ).getName() + globalCfg.getModifiedFlagSuffix();
					}
					else {
						columnName = propertyAuditingData.getExplicitModifiedFlagName();
					}

					MetadataTools.addModifiedFlagPropertyWithColumn(
							parent,
							propertyAuditingData.getName(),
							globalCfg.getModifiedFlagSuffix(),
							propertyAuditingData.getModifiedFlagName(),
							columnName
					);

					return;
				}
			}
		}

		// Default legacy behavior
		super.addModifiedColumns( globalCfg, value, parent, propertyAuditingData );
	}
}
