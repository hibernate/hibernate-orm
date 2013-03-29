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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ForeignKeyContributingSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;
import org.hibernate.metamodel.spi.source.ToOneAttributeSourceNatureResolver;
import org.hibernate.type.ForeignKeyDirection;

/**
 * @author Hardy Ferentschik
 */
public class ToOneAttributeSourceImpl extends SingularAttributeSourceImpl implements ToOneAttributeSource {
	private final AssociationAttribute associationAttribute;
	private final Set<CascadeStyle> cascadeStyles;
	private SingularAttributeSource.Nature nature;

	public ToOneAttributeSourceImpl(SingularAssociationAttribute associationAttribute) {

		super( associationAttribute );
		this.associationAttribute = associationAttribute;
		this.cascadeStyles = EnumConversionHelper.cascadeTypeToCascadeStyleSet(
				associationAttribute.getCascadeTypes(),
				associationAttribute.getHibernateCascadeTypes(),
				associationAttribute.getContext()
		);
		this.nature = determineNatureIfPossible( associationAttribute );
	}

	private static Nature determineNatureIfPossible(SingularAssociationAttribute associationAttribute) {
		if ( MappedAttribute.Nature.MANY_TO_ONE.equals( associationAttribute.getNature() ) ) {
			return Nature.MANY_TO_ONE;
		}
		else if ( MappedAttribute.Nature.ONE_TO_ONE.equals( associationAttribute.getNature() ) ) {
			if ( associationAttribute.getMappedBy() != null ) {
				// This means it is a one-to-one, but it is not working yet.
				//return Nature.ONE_TO_ONE;
				throw new NotYetImplementedException( "@OneToOne with mappedBy specified is not supported yet." );
			}
			else if ( associationAttribute.hasPrimaryKeyJoinColumn() ) {
				return Nature.ONE_TO_ONE;
			}
			else if ( associationAttribute.getJoinTableAnnotation() != null ) {
				return Nature.MANY_TO_ONE;
			}
			else if ( associationAttribute.isId() ) {
				// if this association is part of the ID then this can't be a one-to-one
				return Nature.MANY_TO_ONE;
			}
			else if ( associationAttribute.getJoinColumnValues() == null  ||
					associationAttribute.getJoinColumnValues().isEmpty() ) {
				return Nature.MANY_TO_ONE;
			}
			else {
				return null;
			}
		}
		else {
			throw new AssertionError(String.format( "Wrong attribute nature[%s] for toOne attribute: %s",
					associationAttribute.getNature(), associationAttribute.getRole() ));
		}
	}

	public Nature resolveToOneAttributeSourceNature(ToOneAttributeSourceNatureResolver.ToOneAttributeSourceNatureResolutionContext context) {
		if ( nature != null ) { return nature; }
		final List<org.hibernate.metamodel.spi.relational.Column> idColumns = context.getIdentifierColumns();
		if ( associationAttribute.getJoinColumnValues().size() != idColumns.size() ) {
			nature = Nature.MANY_TO_ONE;
		}
		else {
			Set<String> joinColumnNames = new HashSet<String>( associationAttribute.getJoinColumnValues().size() );
			for ( Column joinColumn : associationAttribute.getJoinColumnValues() ) {
				joinColumnNames.add( joinColumn.getName() );
			}
			// if join columns are the entity's ID, then it is a one-to-one (mapToPk == true)
			boolean areJoinColumnsSameAsIdColumns = true;
			for ( org.hibernate.metamodel.spi.relational.Column idColumn : idColumns ) {
				if ( ! joinColumnNames.contains( idColumn.getColumnName().getText() ) ) {
					areJoinColumnsSameAsIdColumns = false;
					break;
				}
			}
			nature = areJoinColumnsSameAsIdColumns ? Nature.ONE_TO_ONE : Nature.MANY_TO_ONE;
		}
		return nature;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public boolean isUnique() {
		return MappedAttribute.Nature.ONE_TO_ONE.equals( associationAttribute.getNature() );
	}

	@Override
	public boolean isNotFoundAnException() {
		return !associationAttribute.isIgnoreNotFound();
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies(
			final String entityName, final String tableName, final AttributeBinding referencedAttributeBinding) {
		if ( CompositeAttributeBinding.class.isInstance( referencedAttributeBinding ) ) {
			CompositeAttributeBinding compositeAttributeBinding = CompositeAttributeBinding.class.cast(
					referencedAttributeBinding
			);
			List<Binder.DefaultNamingStrategy> result = new ArrayList<Binder.DefaultNamingStrategy>(  );
			for ( final AttributeBinding attributeBinding : compositeAttributeBinding.attributeBindings() ) {
				result.addAll( getDefaultNamingStrategies( entityName, tableName, attributeBinding ) );
			}
			return result;
		}
		else {
			List<Binder.DefaultNamingStrategy> result = new ArrayList<Binder.DefaultNamingStrategy>( 1 );
			result.add(
					new Binder.DefaultNamingStrategy() {
						@Override
						public String defaultName() {
							return associationAttribute.getContext().getNamingStrategy().foreignKeyColumnName(
									associationAttribute.getName(),
									entityName,
									tableName,
									referencedAttributeBinding.getAttribute().getName()
							);
						}
					}
			);
			return result;
		}
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		if ( associationAttribute.getJoinColumnValues().isEmpty() ) {
			return Collections.emptyList();
		}
		List<RelationalValueSource> valueSources = new ArrayList<RelationalValueSource>(
				associationAttribute.getJoinColumnValues()
						.size()
		);
		for ( Column columnValues : associationAttribute.getJoinColumnValues() ) {
			valueSources.add( new ColumnSourceImpl( associationAttribute, null, columnValues ) );
		}
		return valueSources;
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return associationAttribute.getJoinColumnValues()
				.isEmpty() ? null : new AnnotationJoinColumnResolutionDelegate();
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
	public FetchTiming getFetchTiming() {
		return associationAttribute.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE;
	}

	@Override
	public FetchStyle getFetchStyle() {
		if ( associationAttribute.getFetchStyle() != null ) {
			return associationAttribute.getFetchStyle();
		}
		else {
			return associationAttribute.isLazy() ? FetchStyle.SELECT : FetchStyle.JOIN;
		}
	}

	@Override
	public boolean isUnWrapProxy() {
		return associationAttribute.isUnWrapProxy();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ToOneAttributeSourceImpl" );
		sb.append( "{associationAttribute=" ).append( associationAttribute );
		sb.append( ", cascadeStyles=" ).append( cascadeStyles );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return getNature() == Nature.ONE_TO_ONE && !associationAttribute.isOptional() ?
				ForeignKeyDirection.FROM_PARENT :
				ForeignKeyDirection.TO_PARENT;
	}

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
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTable(
					logicalJoinTableName,
					null,
					null
			);
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


