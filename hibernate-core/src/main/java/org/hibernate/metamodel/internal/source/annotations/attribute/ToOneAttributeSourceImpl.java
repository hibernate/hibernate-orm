/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.internal.source.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.SingularAttributeNature;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class ToOneAttributeSourceImpl extends SingularAttributeSourceImpl implements ToOneAttributeSource {
	private final AssociationAttribute associationAttribute;
	private final Set<CascadeStyle> cascadeStyles;

	public ToOneAttributeSourceImpl(AssociationAttribute associationAttribute) {
		super( associationAttribute );
		this.associationAttribute = associationAttribute;
		this.cascadeStyles = EnumConversionHelper.cascadeTypeToCascadeStyleSet( associationAttribute.getCascadeTypes() );
	}

	@Override
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.MANY_TO_ONE;
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return null;  // todo : implement proper method body
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		// Column does not (yet?) deal with formulas at all...
		//
		// not sure how to handle "mixed" cases either.  what happens if some @JoinColumns name
		// a column, but others do not?  For now, lets just throw an error in those cases
		int alternateColumnReferences = 0;
		for ( Column column : associationAttribute.getColumnValues() ) {
			if ( column.getReferencedColumnName() != null ) {
				alternateColumnReferences++;
			}
		}
		if ( alternateColumnReferences == 0 ) {
			return null;
		}
		else {
			if ( alternateColumnReferences != associationAttribute.getColumnValues().size() ) {
				throw associationAttribute.getContext().makeMappingException(
						"Encountered multiple JoinColumns mixing primary-target-columns and alternate-target-columns"
				);
			}
			return new JoinColumnResolutionDelegate() {
				private final String logicalJoinTableName = resolveLogicalJoinTableName();

				@Override
				public List<Value> getJoinColumns(JoinColumnResolutionContext context) {
					final List<Value> values = new ArrayList<Value>();
					for ( Column column : associationAttribute.getColumnValues() ) {
						values.add(
								context.resolveColumn(
										column.getReferencedColumnName(),
										logicalJoinTableName,
										null,
										null
								)
						);
					}
					return values;
				}

				@Override
				public String getReferencedAttributeName() {
					return null;
				}
			};
		}
	}

	private String resolveLogicalJoinTableName() {
		final AnnotationInstance joinTableAnnotation = JandexHelper.getSingleAnnotation(
				associationAttribute.annotations(),
				JPADotNames.JOIN_TABLE
		);

		if ( joinTableAnnotation != null ) {
			return JandexHelper.getValue( joinTableAnnotation, "table", String.class );
		}

		// todo : this ties into the discussion about naming strategies.  This would be part of a logical naming strategy...
		return null;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return null;
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}

	@Override
	public FetchMode getFetchMode() {
		return associationAttribute.getFetchMode();
	}

	@Override
	public FetchTiming getFetchTiming() {
		if(associationAttribute.isLazy()) {
			return FetchTiming.DELAYED;
		}  else {
			return FetchTiming.IMMEDIATE;
		}
	}

	@Override
	public FetchStyle getFetchStyle() {
		return associationAttribute.getFetchStyle();
	}
}


