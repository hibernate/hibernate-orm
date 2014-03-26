/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.internal.binder.BinderRootContext;
import org.hibernate.metamodel.internal.binder.ForeignKeyHelper;
import org.hibernate.metamodel.internal.binder.ManyToManyCollectionTableNamingStrategyHelper;
import org.hibernate.metamodel.internal.binder.RelationalValueBindingHelper;
import org.hibernate.metamodel.internal.binder.TableHelper;
import org.hibernate.metamodel.source.spi.AssociationSource;
import org.hibernate.metamodel.source.spi.ForeignKeyContributingSource;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.metamodel.source.spi.PluralAttributeKeySource;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSourceContainer;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.TableSpecificationSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.type.ForeignKeyDirection;

/**
 + * @author Gail Badner
 + */
public class StandardAssociationRelationalBindingResolverImpl implements AssociationRelationalBindingResolver {
	private final BinderRootContext helperContext;

	public StandardAssociationRelationalBindingResolverImpl(BinderRootContext helperContext) {
		this.helperContext = helperContext;
	}

	@Override
	public SingularAttributeBinding resolveOneToOneReferencedAttributeBinding(
			ToOneAttributeSource attributeSource,
			EntityBinding referencedEntityBinding) {
		return resolveReferencedAttributeBinding( attributeSource, referencedEntityBinding );
	}

	@Override
	public List<RelationalValueBinding> resolveOneToOneRelationalValueBindings(
			final ToOneAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			SingularAttributeBinding referencedAttributeBinding) {
		if ( ! attributeSource.relationalValueSources().isEmpty() ) {
			final TableSpecification defaultTable =
					locateDefaultTableSpecificationForAttribute(
							attributeBindingContainer,
							attributeSource
					);
			return resolveRelationalValueBindings(
					attributeSource,
					attributeBindingContainer.seekEntityBinding(),
					defaultTable,
					false,
					attributeSource.getDefaultNamingStrategies(
							attributeBindingContainer.seekEntityBinding().getEntityName(),
							defaultTable.getLogicalName().getText(),
							referencedAttributeBinding
					)
			);
		}
		else {
			return Collections.emptyList();
		}
	}

	private TableSpecification locateDefaultTableSpecificationForAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource) {
		return attributeSource.getContainingTableName() == null ?
				attributeBindingContainer.getPrimaryTable() :
				attributeBindingContainer.seekEntityBinding().locateTable( attributeSource.getContainingTableName() );
	}


	@Override
	public ForeignKey resolveOneToOneForeignKey(
			ToOneAttributeSource attributeSource,
			TableSpecification sourceTable,
			List<Column> sourceColumns,
			EntityBinding referencedEntityBinding) {
		if ( attributeSource.getForeignKeyDirection() == ForeignKeyDirection.TO_PARENT ) {
			throw new AssertionFailure( "Cannot create a foreign key for one-to-one with foreign key direction going to the parent." );
		}

		final TableSpecification targetTable = foreignKeyHelper().determineForeignKeyTargetTable(
				referencedEntityBinding,
				attributeSource
		);
		final List<Column> targetColumns = foreignKeyHelper().determineForeignKeyTargetColumns(
				referencedEntityBinding,
				attributeSource
		);
		return foreignKeyHelper().locateOrCreateForeignKey(
				attributeSource.getExplicitForeignKeyName(),
				sourceTable,
				sourceColumns,
				targetTable,
				targetColumns,
				attributeSource.isCascadeDeleteEnabled(),
				attributeSource.createForeignKeyConstraint()
		);
	}

	@Override
	public SingularAttributeBinding resolveManyToOneReferencedAttributeBinding(
			AttributeBindingContainer attributeBindingContainer,
			ToOneAttributeSource attributeSource,
			EntityBinding referencedEntityBinding) {
		return resolveReferencedAttributeBinding( attributeSource, referencedEntityBinding );
	}

	@Override
	public List<RelationalValueBinding> resolveManyToOneRelationalValueBindings(
			ToOneAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			SingularAttributeBinding referencedAttributeBinding,
			EntityBinding referencedEntityBinding) {
		final TableSpecification defaultTable =
				locateDefaultTableSpecificationForAttribute(
						attributeBindingContainer,
						attributeSource
				);
		return resolveRelationalValueBindings(
				attributeSource,
				attributeBindingContainer.seekEntityBinding(),
				defaultTable,
				false,
				attributeSource.getDefaultNamingStrategies(
						attributeBindingContainer.seekEntityBinding().getEntityName(),
						defaultTable.getLogicalName().getText(),
						referencedAttributeBinding
				)
		);

	}

	@Override
	public ForeignKey resolveManyToOneForeignKey(
			ToOneAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			List<RelationalValueBinding> relationalValueBindings,
			EntityBinding referencedEntityBinding) {
		final List<Column> targetColumns = foreignKeyHelper().determineForeignKeyTargetColumns(
				referencedEntityBinding,
				attributeSource
		);
		return locateOrCreateForeignKey(
				attributeSource,
				referencedEntityBinding,
				relationalValueBindings.get( 0 ).getTable(),
				relationalValueBindings,
				targetColumns
		);
	}

	@Override
	public List<RelationalValueBinding> resolveManyToManyElementRelationalValueBindings(
			final EntityBinding entityBinding,
			final PluralAttributeElementSourceManyToMany elementSource,
			final TableSpecification collectionTable,
			final EntityBinding referencedEntityBinding) {
		final List<Column> targetColumns =
				foreignKeyHelper().determineForeignKeyTargetColumns(
						referencedEntityBinding,
						elementSource
				);
		final List<Binder.DefaultNamingStrategy> namingStrategies = new ArrayList<Binder.DefaultNamingStrategy>( targetColumns.size() );
		for ( final Column targetColumn : targetColumns ) {
			namingStrategies.add(
					new Binder.DefaultNamingStrategy() {
						@Override
						public String defaultName(NamingStrategy namingStrategy) {
							return namingStrategy.foreignKeyColumnName(
									elementSource.getAttributeSource().getName(),
									referencedEntityBinding.getEntityName(),
									referencedEntityBinding.getPrimaryTableName(),
									targetColumn.getColumnName().getText()
							);
						}
					}
			);
		}
		return resolveRelationalValueBindings(
				elementSource,
				entityBinding,
				collectionTable,
				true,
				namingStrategies
		);
	}

	@Override
	public ForeignKey resolveManyToManyElementForeignKey(
			final EntityBinding entityBinding,
			final PluralAttributeElementSourceManyToMany elementSource,
			final TableSpecification collectionTable,
			final List<RelationalValueBinding> relationalValueBindings,
			final EntityBinding referencedEntityBinding) {
		final List<Column> targetColumns =
				foreignKeyHelper().determineForeignKeyTargetColumns(
						referencedEntityBinding,
						elementSource
				);
		return locateOrCreateForeignKey(
				elementSource,
				referencedEntityBinding,
				collectionTable,
				relationalValueBindings,
				targetColumns
		);
	}

	@Override
	public TableSpecification resolveManyToManyCollectionTable(
			PluralAttributeSource pluralAttributeSource,
			String attributePath,
			EntityBinding entityBinding,
			EntityBinding referencedEntityBinding) {

		final TableSpecificationSource collectionTableSource = pluralAttributeSource.getCollectionTableSpecificationSource();
		return tableHelper().createTable(
				collectionTableSource,
				new ManyToManyCollectionTableNamingStrategyHelper(
						attributePath,
						pluralAttributeSource.isInverse(),
						entityBinding,
						referencedEntityBinding
				)
		);
	}

	@Override
	public List<RelationalValueBinding> resolvePluralAttributeKeyRelationalValueBindings(
			final PluralAttributeSource attributeSource,
			final EntityBinding entityBinding,
			final TableSpecification collectionTable,
			final EntityBinding referencedEntityBinding) {
		final PluralAttributeKeySource keySource = attributeSource.getKeySource();

		final List<Column>targetColumns = foreignKeyHelper().determineForeignKeyTargetColumns(
				referencedEntityBinding,
				keySource
		);
		final List<Binder.DefaultNamingStrategy> namingStrategies = new ArrayList<Binder.DefaultNamingStrategy>( targetColumns.size() );

		final String ownedAttributeName;
		if ( attributeSource.getElementSource().getNature().isAssociation() ) {
			final AssociationSource associationSource = (AssociationSource) attributeSource.getElementSource();
			if ( associationSource.getOwnedAssociationSources().size() > 1 ) {
				throw new NotYetImplementedException( "Cannot determine default naming strategy when an association owns more than 1 other association." );
			}
			if ( associationSource.getOwnedAssociationSources().isEmpty() ) {
				ownedAttributeName = null;
			}
			else {
				final AssociationSource ownedAssociationSource = associationSource.getOwnedAssociationSources().iterator().next();
				ownedAttributeName =  ownedAssociationSource.getAttributeSource().getName();
			}
		}
		else {
			ownedAttributeName = null;
		}

		for ( final Column targetColumn : targetColumns ) {
			namingStrategies.add(
					new Binder.DefaultNamingStrategy() {
						@Override
						public String defaultName(NamingStrategy namingStrategy) {
							return namingStrategy.foreignKeyColumnName(
									ownedAttributeName,
									entityBinding.getEntityName(),
									entityBinding.getPrimaryTableName(),
									targetColumn.getColumnName().getText()
							);
						}
					}
			);
		}
		return resolveRelationalValueBindings(
				keySource,
				entityBinding,
				collectionTable,
				attributeSource.getElementSource().getNature() != PluralAttributeElementNature.ONE_TO_MANY,
				namingStrategies
		);
	}

	@Override
	public ForeignKey resolvePluralAttributeKeyForeignKey(
			final PluralAttributeSource attributeSource,
			final EntityBinding entityBinding,
			final TableSpecification collectionTable,
			final List<RelationalValueBinding> sourceRelationalValueBindings,
			final EntityBinding referencedEntityBinding) {
		final PluralAttributeKeySource keySource = attributeSource.getKeySource();
		List<Column> targetColumns =
				foreignKeyHelper().determineForeignKeyTargetColumns(
						referencedEntityBinding,
						keySource
				);
		return locateOrCreateForeignKey(
				keySource,
				referencedEntityBinding,
				collectionTable,
				sourceRelationalValueBindings,
				targetColumns
		);
	}

	@Override
	public SingularAttributeBinding resolvePluralAttributeKeyReferencedBinding(
			AttributeBindingContainer attributeBindingContainer,
			PluralAttributeSource attributeSource) {
		return foreignKeyHelper().determineReferencedAttributeBinding(
				attributeSource.getKeySource(),
				attributeBindingContainer.seekEntityBinding()
		);
	}


	private SingularAttributeBinding resolveReferencedAttributeBinding(
			ToOneAttributeSource attributeSource,
			EntityBinding referencedEntityBinding) {
		return foreignKeyHelper().determineReferencedAttributeBinding( attributeSource, referencedEntityBinding );
	}

	public List<RelationalValueBinding> resolveRelationalValueBindings(
			final RelationalValueSourceContainer relationalValueSourceContainer,
			EntityBinding entityBinding,
			TableSpecification defaultTable,
			boolean forceNonNullable,
			List<Binder.DefaultNamingStrategy> defaultNamingStrategies) {
		return relationalValueBindingHelper().createRelationalValueBindings(
				entityBinding,
				relationalValueSourceContainer,
				defaultTable,
				defaultNamingStrategies,
				forceNonNullable
		);
	}

	private ForeignKey locateOrCreateForeignKey(
			final ForeignKeyContributingSource foreignKeyContributingSource,
			final EntityBinding referencedEntityBinding,
			final TableSpecification sourceTable,
			final List<RelationalValueBinding> sourceRelationalValueBindings,
			final List<Column> targetColumns) {
		final TableSpecification targetTable = foreignKeyHelper().determineForeignKeyTargetTable(
				referencedEntityBinding,
				foreignKeyContributingSource
		);
		return foreignKeyHelper().locateOrCreateForeignKey(
				foreignKeyContributingSource.getExplicitForeignKeyName(),
				sourceTable,
				extractColumnsFromRelationalValueBindings( sourceRelationalValueBindings ),
				targetTable,
				targetColumns,
				foreignKeyContributingSource.isCascadeDeleteEnabled(),
				foreignKeyContributingSource.createForeignKeyConstraint()
		);
	}

	private TableHelper tableHelper() {
		return helperContext.tableHelper();
	}

	private ForeignKeyHelper foreignKeyHelper() {
		return helperContext.foreignKeyHelper();
	}

	private RelationalValueBindingHelper relationalValueBindingHelper() {
		return helperContext.relationalValueBindingHelper();
	}

	// TODO: try to get rid of this...
	private static List<Column> extractColumnsFromRelationalValueBindings(
			final List<RelationalValueBinding> valueBindings) {
		List<Column> columns = new ArrayList<Column>( valueBindings.size() );
		for ( RelationalValueBinding relationalValueBinding : valueBindings ) {
			final Value value = relationalValueBinding.getValue();
			// todo : currently formulas are not supported here... :(
			if ( !Column.class.isInstance( value ) ) {
				throw new NotYetImplementedException(
						"Derived values are not supported when creating a foreign key that targets columns."
				);
			}
			columns.add( (Column) value );
		}
		return columns;
	}

}
