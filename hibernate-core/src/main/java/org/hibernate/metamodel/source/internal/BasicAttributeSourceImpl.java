/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.source.internal.annotations.ColumnSourceImpl;
import org.hibernate.metamodel.source.internal.annotations.DerivedValueSourceImpl;
import org.hibernate.metamodel.source.internal.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;

/**
 * Represents source of a basic singular attribute information for binding
 *
 * @author Steve Ebersole
 */
public class BasicAttributeSourceImpl extends AbstractSingularAttributeSource {
	private final List<RelationalValueSource> relationalValueSources;

	protected BasicAttributeSourceImpl(AbstractManagedTypeSource container, BasicAttribute attribute) {
		super( container, attribute );

		if ( container.locateAttributeOverride( attribute.getName() ) != null ) {
			throw container.getLocalBindingContext().makeMappingException(
					"Association-override not valid on basic attributes : "
							+ attribute.getBackingMember().toString()
			);
		}


		this.relationalValueSources = new ArrayList<RelationalValueSource>();
		if ( attribute.getFormulaValue() != null ) {
			relationalValueSources.add( new DerivedValueSourceImpl( attribute.getFormulaValue() ) );
		}
		else {
			final AttributeOverride attributeOverride = container.locateAttributeOverride( attribute.getName() );
			final int explicitColumnCount = attribute.getColumnValues().size();

			if ( explicitColumnCount == 0 ) {
				Column overrideColumn = attributeOverride.getImpliedColumn();
				if ( overrideColumn != null
						|| attribute.getCustomReadFragment() != null
						|| attribute.getCustomWriteFragment() != null
						|| attribute.getCheckCondition() != null ) {
					relationalValueSources.add(
							new ColumnSourceImpl(
									overrideColumn,
									null,
									attribute.getCustomReadFragment(),
									attribute.getCustomWriteFragment(),
									attribute.getCheckCondition()
							)
					);
				}
			}
			else if ( explicitColumnCount == 1 ) {
				Column column = attribute.getColumnValues().get( 0 );
				if ( attributeOverride != null ) {
					column.applyColumnValues( attributeOverride.getOverriddenColumnInfo() );
				}
				relationalValueSources.add(
						new ColumnSourceImpl(
								column,
								null,
								attribute.getCustomReadFragment(),
								attribute.getCustomWriteFragment(),
								attribute.getCheckCondition()
						)
				);
			}
			else {
				if ( attributeOverride != null ) {
					throw container.getLocalBindingContext().makeMappingException(
							"Cannot apply AttributeOverride to attribute mapped to more than one column : "
									+ attribute.getBackingMember().toString()
					);
				}

				for ( Column column : attribute.getColumnValues() ) {
					relationalValueSources.add( new ColumnSourceImpl( column, null ) );
				}
			}
		}
	}

	@Override
	public BasicAttribute getPersistentAttribute() {
		return (BasicAttribute) super.getPersistentAttribute();
	}

	@Override
	protected void validateConversionInfo(AttributeConversionInfo conversionInfo) {

	}

	@Override
	public Nature getNature() {
		return Nature.BASIC;
	}

	@Override
	public String getContainingTableName() {
		return null;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return null;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return relationalValueSources;
	}
}
