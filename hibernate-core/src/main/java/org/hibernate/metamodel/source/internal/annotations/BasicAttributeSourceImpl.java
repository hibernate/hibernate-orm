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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.source.internal.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.OverrideAndConverterCollector;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.SingularAttributeNature;

/**
 * @author Steve Ebersole
 */
public class BasicAttributeSourceImpl extends SingularAttributeSourceImpl {
	private final List<RelationalValueSource> relationalValueSources;

	public BasicAttributeSourceImpl(
			BasicAttribute attribute,
			OverrideAndConverterCollector overrideAndConverterCollector) {
		super( attribute );

		final AttributeOverride override = overrideAndConverterCollector.locateAttributeOverride(
				attribute.getPath()
		);
		validateAttributeOverride( override );

		this.relationalValueSources = buildRelationalValueSources( attribute, override );
	}

	protected void validateAttributeOverride(AttributeOverride override) {

	}

	@Override
	public BasicAttribute getAnnotatedAttribute() {
		return (BasicAttribute) super.getAnnotatedAttribute();
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return relationalValueSources;
	}

	private List<RelationalValueSource> buildRelationalValueSources(
			BasicAttribute attribute,
			AttributeOverride attributeOverride) {
		final List<RelationalValueSource> relationalValueSources = new ArrayList<RelationalValueSource>();
		if ( attribute.getFormulaValue() != null ) {
			relationalValueSources.add( new DerivedValueSourceImpl( attribute.getFormulaValue() ) );
		}
		else {
			final int explicitColumnCount = attribute.getColumnValues().size();

			if ( explicitColumnCount == 0 ) {
				Column overrideColumn = attributeOverride == null ? null : attributeOverride.getImpliedColumn();
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
					throw attribute.getContainer().getLocalBindingContext().makeMappingException(
							"Cannot apply AttributeOverride to attribute mapped to more than one column : "
									+ attribute.getBackingMember().toString()
					);
				}

				for ( Column column : attribute.getColumnValues() ) {
					relationalValueSources.add( new ColumnSourceImpl( column, null ) );
				}
			}
		}

		return relationalValueSources;
	}

	@Override
	public AttributePath getAttributePath() {
		return getAnnotatedAttribute().getPath();
	}

	@Override
	public AttributeRole getAttributeRole() {
		return getAnnotatedAttribute().getRole();
	}
}
