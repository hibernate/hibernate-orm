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

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AttributeSourceResolutionContext;
import org.hibernate.metamodel.spi.source.ForeignKeyContributingSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class ToOneAttributeSourceImpl extends AbstractToOneAttributeSourceImpl implements ToOneAttributeSource {
	private final List<RelationalValueSource> relationalValueSources;
	private final String containingTableName;
	private final EntityBindingContext bindingContext;

	public ToOneAttributeSourceImpl(SingularAssociationAttribute associationAttribute, String relativePath,
			EntityBindingContext bindingContext) {
		super( associationAttribute, relativePath );
		if ( associationAttribute.getMappedBy() != null ) {
			throw new IllegalArgumentException( "associationAttribute.getMappedBy() must be null" );
		}
		this.bindingContext = bindingContext;
		// Need to initialize relationalValueSources before determining logicalJoinTableName.
		this.relationalValueSources = resolveRelationalValueSources( associationAttribute );
		// Need to initialize logicalJoinTableName before determining nature.
		this.containingTableName = resolveContainingTableName( associationAttribute, relationalValueSources );
		setNature( determineNatureIfPossible( associationAttribute ) );
	}

	private Nature determineNatureIfPossible(
			SingularAssociationAttribute associationAttribute) {
		if ( MappedAttribute.Nature.MANY_TO_ONE.equals( associationAttribute.getNature() ) ) {
			return Nature.MANY_TO_ONE;
		}
		else if ( MappedAttribute.Nature.ONE_TO_ONE.equals( associationAttribute.getNature() ) ) {
			if ( getContainingTableName() != null ) {
				return Nature.MANY_TO_ONE;
			}
			else if ( associationAttribute.hasPrimaryKeyJoinColumn() ) {
				return Nature.ONE_TO_ONE;
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
	@Override
	public void resolveToOneAttributeSource(AttributeSourceResolutionContext context) {
		if ( getNature() != null ) {
			return;
		}
		// It would be nice to have the following block in determineNatureIfPossible(),
		// but it requires context.resolveIdentifierColumns(), it's here instead.
		if ( MappedAttribute.Nature.ONE_TO_ONE.equals( associationAttribute().getNature() ) ) {
			final List<org.hibernate.metamodel.spi.relational.Column> idColumns = context.resolveIdentifierColumns();
			if ( associationAttribute().getJoinColumnValues().size() != idColumns.size() ) {
				setNature( Nature.MANY_TO_ONE );
			}
			else {
				Set<String> joinColumnNames = new HashSet<String>( associationAttribute().getJoinColumnValues().size() );
				for ( Column joinColumn : associationAttribute().getJoinColumnValues() ) {
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
				setNature( areJoinColumnsSameAsIdColumns ? Nature.ONE_TO_ONE : Nature.MANY_TO_ONE );
			}
		}
		if ( getNature() == null ) {
			throw new NotYetImplementedException( "unknown type of to-one attribute." );
		}
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
						public String defaultName(NamingStrategy namingStrategy) {
							return namingStrategy.foreignKeyColumnName(
									associationAttribute().getName(),
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
		return relationalValueSources;
	}

	@Override
	public String getContainingTableName() {
		return containingTableName;
	}

	private List<RelationalValueSource> resolveRelationalValueSources(AssociationAttribute associationAttribute) {
		final List<RelationalValueSource> valueSources;
		final List<Column> joinColumns;
		if ( associationAttribute.getJoinTableAnnotation() == null ) {
			joinColumns = associationAttribute.getJoinColumnValues();
		}
		else {
			joinColumns = associationAttribute.getInverseJoinColumnValues();
		}
		if ( joinColumns.isEmpty() ) {
			valueSources = Collections.emptyList();
		}
		else {
			valueSources = new ArrayList<RelationalValueSource>( joinColumns.size() );
			for ( Column joinColumn : joinColumns ) {
				valueSources.add(
						new ColumnSourceImpl(
								associationAttribute,
								joinColumn,
								getDefaultLogicalJoinTableName( associationAttribute )
						)
				);
			}
		}
		return valueSources;
	}

	private String getDefaultLogicalJoinTableName(AssociationAttribute associationAttribute) {
		if ( associationAttribute.getJoinTableAnnotation() == null ) {
			return null;
		}
		return JandexHelper.getValue( associationAttribute.getJoinTableAnnotation(), "name", String.class,
				bindingContext.getServiceRegistry().getService( ClassLoaderService.class ));
	}


	private String resolveContainingTableName(
			AssociationAttribute associationAttribute,
			List<RelationalValueSource> relationalValueSources) {
		if ( relationalValueSources.isEmpty() ) {
			return getDefaultLogicalJoinTableName( associationAttribute );
		}
		String logicalTableName = relationalValueSources.get( 0 ).getContainingTableName();
		for ( int i = 1; i< relationalValueSources.size(); i++ ) {
			if ( logicalTableName == null ) {
				if ( relationalValueSources.get( i ).getContainingTableName() != null ) {
					throw new IllegalStateException( "Relational value sources refer to null and non-null containing tables." );
				}
			}
			else if ( !logicalTableName.equals( relationalValueSources.get( i ).getContainingTableName() ) ) {
				throw new IllegalStateException( "Relational value sources do not refer to the same containing table." );
			}
		}
		return logicalTableName;
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		List<Column> joinColumns =
				associationAttribute().getJoinTableAnnotation() == null ?
						associationAttribute().getJoinColumnValues() :
						associationAttribute().getInverseJoinColumnValues();
		boolean hasReferencedColumn = false;
		for ( Column joinColumn : joinColumns ) {
			if ( joinColumn.getReferencedColumnName() != null ) {
				hasReferencedColumn = true;
				break;
			}
		}
		return hasReferencedColumn ? new AnnotationJoinColumnResolutionDelegate() : null;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return null;
	}

	public class AnnotationJoinColumnResolutionDelegate
			implements ForeignKeyContributingSource.JoinColumnResolutionDelegate {

		@Override
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
			final List<Value> values = new ArrayList<Value>();
			final List<Column> joinColumns =
					associationAttribute().getJoinTableAnnotation() == null ?
							associationAttribute().getJoinColumnValues() :
							associationAttribute().getInverseJoinColumnValues();
			for ( Column joinColumn : joinColumns ) {
				if ( joinColumn.getReferencedColumnName() == null ) {
					return context.resolveRelationalValuesForAttribute( null );
				}
				org.hibernate.metamodel.spi.relational.Column resolvedColumn = context.resolveColumn(
						joinColumn.getReferencedColumnName(),
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
			return context.resolveTable(
					null,
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
					associationAttribute().annotations(),
					JPADotNames.JOIN_TABLE
			);

			if ( joinTableAnnotation != null ) {
				return JandexHelper.getValue( joinTableAnnotation, "name", String.class,
						bindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );
			}

			// todo : this ties into the discussion about naming strategies.  This would be part of a logical naming strategy...
			return null;
		}
	}
}


