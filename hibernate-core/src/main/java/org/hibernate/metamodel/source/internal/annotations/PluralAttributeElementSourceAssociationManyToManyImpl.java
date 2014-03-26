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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.ForeignKeyContributingSource;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Hardy Ferentschik
 * @author Brett Meyer
 * @author Gail Badner
 */
public class PluralAttributeElementSourceAssociationManyToManyImpl
		extends AbstractPluralAttributeElementSourceAssociationManyToManyImpl
		implements PluralAttributeElementSourceManyToMany {

	private final List<RelationalValueSource> relationalValueSources = new ArrayList<RelationalValueSource>();

	public PluralAttributeElementSourceAssociationManyToManyImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		super( pluralAttributeSource );
		if ( pluralAttributeSource.getMappedBy() != null ) {
			throw new AssertionFailure( "pluralAttributeSource.getMappedByAttributeName() must be null." );
		}
		for ( Column column : pluralAttributeSource.pluralAssociationAttribute().getInverseJoinColumnValues() ) {
			relationalValueSources.add( new ColumnSourceImpl( column ) );
		}
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return relationalValueSources;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return pluralAssociationAttribute().getInverseForeignKeyName();
	}

	@Override
	public boolean createForeignKeyConstraint() {
		return pluralAssociationAttribute().createForeignKeyConstraint();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		boolean hasReferencedColumn = false;
		for ( Column joinColumn : pluralAssociationAttribute().getInverseJoinColumnValues() ) {
			if ( joinColumn.getReferencedColumnName() != null ) {
				hasReferencedColumn = true;
				break;
			}
		}
		return hasReferencedColumn ?
				new AnnotationJoinColumnResolutionDelegate() :
				null;
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

	// TODO: This needs reworked.
	public class AnnotationJoinColumnResolutionDelegate
			implements ForeignKeyContributingSource.JoinColumnResolutionDelegate {
		private final String logicalJoinTableName;

		public AnnotationJoinColumnResolutionDelegate() {
			logicalJoinTableName = resolveLogicalJoinTableName();
		}

		@Override
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
			final List<Value> values = new ArrayList<Value>();
			for ( Column column : pluralAssociationAttribute().getInverseJoinColumnValues() ) {
				org.hibernate.metamodel.spi.relational.Column resolvedColumn = context.resolveColumn(
						column.getReferencedColumnName(),
						null,
						null,
						null
				);
				values.add( resolvedColumn );
			}
			return values;
		}

		@Override
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTable( null, null, null );
		}

		@Override
		public String getReferencedAttributeName() {
			// HBM only
			return null;
		}

		private String resolveLogicalJoinTableName() {
			final AnnotationInstance joinTableAnnotation = getPluralAttribute().getBackingMember()
					.getAnnotations()
					.get( JPADotNames.JOIN_TABLE );

			if ( joinTableAnnotation != null ) {
				final AnnotationValue nameValue = joinTableAnnotation.value( "name" );
				if ( nameValue != null ) {
					return StringHelper.nullIfEmpty( nameValue.asString() );
				}
			}

			// todo : this ties into the discussion about naming strategies.  This would be part of a logical naming strategy...
			return null;
		}
	}
}


