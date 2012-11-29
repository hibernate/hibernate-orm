/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.CascadeType;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ForeignKeyContributingSource;
import org.hibernate.metamodel.spi.source.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class ManyToManyPluralAttributeElementSourceImpl implements ManyToManyPluralAttributeElementSource {
	
	private final PluralAssociationAttribute associationAttribute;
	private final List<RelationalValueSource> relationalValueSources
			= new ArrayList<RelationalValueSource>();
	private final Collection<String> referencedColumnNames
			= new HashSet<String>();
	private final Iterable<CascadeStyle> cascadeStyles;

	public ManyToManyPluralAttributeElementSourceImpl(
			PluralAssociationAttribute associationAttribute) {
		this.associationAttribute = associationAttribute;
		
		for ( Column column : associationAttribute.getJoinColumnValues() ) {
			relationalValueSources.add( new ColumnSourceImpl( 
					associationAttribute, null, column ) );
		}
		
		for ( Column column : associationAttribute.getJoinColumnValues() ) {
			if ( column.getReferencedColumnName() != null ) {
				referencedColumnNames.add( column.getReferencedColumnName() );
			}
		}
		
		cascadeStyles = EnumConversionHelper.cascadeTypeToCascadeStyleSet(
				associationAttribute.getCascadeTypes(),
				associationAttribute.getContext() );
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public String getReferencedEntityAttributeName() {
		// JPA does not have the concept of property refs. Instead column names via @JoinColumn are used
		return null;
	}

	@Override
	public Collection<String> getReferencedColumnNames() {
		return referencedColumnNames;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return relationalValueSources;
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}

	@Override
	public boolean isNotFoundAnException() {
		return !associationAttribute.isIgnoreNotFound();
	}

	@Override
	public String getExplicitForeignKeyName() {
		return associationAttribute.getInverseForeignKeyName();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return associationAttribute.getJoinColumnValues()
				.isEmpty() ? null : new AnnotationJoinColumnResolutionDelegate();
	}

	@Override
	public boolean isUnique() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getOrderBy() {
		return associationAttribute.getOrderBy();
	}

	@Override
	public String getWhere() {
		return associationAttribute.getWhereClause();
	}

	@Override
	public boolean fetchImmediately() {
		return associationAttribute.isLazy();
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_MANY;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}

	// TODO: Taken from and in duplicate of ToOneAttributeSourceImpl.  Look into
	// abstracting some of this out.
	public class AnnotationJoinColumnResolutionDelegate
			implements ForeignKeyContributingSource.JoinColumnResolutionDelegate {
		private final String logicalJoinTableName;

		public AnnotationJoinColumnResolutionDelegate() {
			logicalJoinTableName = resolveLogicalJoinTableName();
		}

		@Override
		public List<Value> getJoinColumns(JoinColumnResolutionContext context) {
			final List<Value> values = new ArrayList<Value>();
			for ( Column column : associationAttribute.getJoinColumnValues() ) {
				if ( column.getReferencedColumnName() == null ) {
					return context.resolveRelationalValuesForAttribute( null );
				}
				org.hibernate.metamodel.spi.relational.Column resolvedColumn = context.resolveColumn(
						column.getReferencedColumnName(),
						logicalJoinTableName,
						null,
						null
				);
				values.add( resolvedColumn );
			}
			return values;
		}

		@Override
		public String getReferencedAttributeName() {
			// in annotations we are not referencing attribute but column names via @JoinColumn(s)
			return null;
		}

		private String resolveLogicalJoinTableName() {
			final AnnotationInstance joinTableAnnotation = JandexHelper.getSingleAnnotation(
					associationAttribute.annotations(),
					JPADotNames.JOIN_TABLE
			);

			if ( joinTableAnnotation != null ) {
				return JandexHelper.getValue( joinTableAnnotation, "name", String.class );
			}

			// todo : this ties into the discussion about naming strategies.  This would be part of a logical naming strategy...
			return null;
		}
	}
}


