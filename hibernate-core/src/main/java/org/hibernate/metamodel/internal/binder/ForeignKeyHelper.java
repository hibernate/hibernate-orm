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
package org.hibernate.metamodel.internal.binder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.binder.ConstraintNamingStrategyHelper.ForeignKeyNamingStrategyHelper;
import org.hibernate.metamodel.source.spi.ForeignKeyContributingSource;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Gail Badner
 * @author Brett Meyer
 */
public class ForeignKeyHelper {
	private final BinderRootContext helperContext;

	public ForeignKeyHelper(BinderRootContext helperContext) {
		this.helperContext = helperContext;
	}

	public List<Column> determineForeignKeyTargetColumns(
			final EntityBinding entityBinding,
			final ForeignKeyContributingSource foreignKeyContributingSource) {

		// TODO: This method, JoinColumnResolutionContext,
		// and JoinColumnResolutionDelegate need re-worked.  There is currently
		// no way to bind to a collection's inverse foreign key.

		final ForeignKeyContributingSource.JoinColumnResolutionDelegate fkColumnResolutionDelegate =
				foreignKeyContributingSource.getForeignKeyTargetColumnResolutionDelegate();

		if ( fkColumnResolutionDelegate == null ) {
			return entityBinding.getPrimaryTable().getPrimaryKey().getColumns();
		}
		else {
			final List<Column> columns = new ArrayList<Column>();
			final ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext = new JoinColumnResolutionContextImpl( entityBinding );
			for ( Value relationalValue : fkColumnResolutionDelegate.getJoinColumns( resolutionContext ) ) {
				if ( !Column.class.isInstance( relationalValue ) ) {
					throw bindingContext().makeMappingException(
							"Foreign keys can currently only name columns, not formulas"
					);
				}
				columns.add( (Column) relationalValue );
			}
			return columns;
		}
	}

	public TableSpecification determineForeignKeyTargetTable(
			final EntityBinding entityBinding,
			final ForeignKeyContributingSource foreignKeyContributingSource) {

		final ForeignKeyContributingSource.JoinColumnResolutionDelegate fkColumnResolutionDelegate =
				foreignKeyContributingSource.getForeignKeyTargetColumnResolutionDelegate();
		if ( fkColumnResolutionDelegate == null ) {
			return entityBinding.getPrimaryTable();
		}
		else {
			final ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext = new JoinColumnResolutionContextImpl( entityBinding );
			return fkColumnResolutionDelegate.getReferencedTable( resolutionContext );
		}
	}

	public SingularAttributeBinding determineReferencedAttributeBinding(
			final ForeignKeyContributingSource foreignKeyContributingSource,
			final EntityBinding referencedEntityBinding) {
		// todo : this is definitely the place that leads to problems with @Id @ManyToOne

		final ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate =
				foreignKeyContributingSource.getForeignKeyTargetColumnResolutionDelegate();
		if ( resolutionDelegate == null ) {
			return referencedEntityBinding.getHierarchyDetails().getEntityIdentifier()
					.getEntityIdentifierBinding()
					.getAttributeBinding();
		}

		final ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext =
				new JoinColumnResolutionContextImpl( referencedEntityBinding );

		final String explicitName = resolutionDelegate.getReferencedAttributeName();
		final AttributeBinding referencedAttributeBinding;
		if ( explicitName != null ) {
			referencedAttributeBinding = referencedEntityBinding.locateAttributeBindingByPath( explicitName, true );
		}
		else {
			referencedAttributeBinding = referencedEntityBinding.locateAttributeBinding(
					resolutionDelegate.getReferencedTable( resolutionContext ),
					resolutionDelegate.getJoinColumns( resolutionContext ),
					true
			);
		}

		if ( referencedAttributeBinding == null ) {
			if ( explicitName != null ) {
				throw bindingContext().makeMappingException(
						String.format(
								"No attribute binding found with name: %s.%s",
								referencedEntityBinding.getEntityName(),
								explicitName
						)
				);
			}
			else {
				throw new NotYetImplementedException(
						"No support yet for referenced join columns unless they correspond with columns bound for an attribute binding."
				);
			}
		}

		if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
			throw bindingContext().makeMappingException(
					String.format(
							"Foreign key references a non-singular attribute [%s]",
							referencedAttributeBinding.getAttribute().getName()
					)
			);
		}
		return (SingularAttributeBinding) referencedAttributeBinding;
	}

	public ForeignKey locateOrCreateForeignKey(
			String explicitForeignKeyName,
			final TableSpecification sourceTable,
			final List<Column> sourceColumns,
			final TableSpecification targetTable,
			final List<Column> targetColumns,
			boolean isCascadeDeleteEnabled,
			boolean createConstraint) {
		final String foreignKeyName = helperContext.relationalIdentifierHelper().normalizeDatabaseIdentifier(
				explicitForeignKeyName, new ForeignKeyNamingStrategyHelper(
						sourceTable, sourceColumns, targetTable, targetColumns ) );
		
		ForeignKey foreignKey = locateAndBindForeignKeyByName( foreignKeyName, sourceTable, sourceColumns, targetTable, targetColumns );
		if ( foreignKey == null ) {
			// no foreign key found; create one
			foreignKey = sourceTable.createForeignKey( targetTable, foreignKeyName, createConstraint );
			bindForeignKeyColumns( foreignKey, sourceTable, sourceColumns, targetTable, targetColumns );
		}
		if ( isCascadeDeleteEnabled ) {
			foreignKey.setDeleteRule( ForeignKey.ReferentialAction.CASCADE );
		}
		return foreignKey;
	}

	private void bindForeignKeyColumns(
			final ForeignKey foreignKey,
			final TableSpecification sourceTable,
			final List<Column> sourceColumns,
			final TableSpecification targetTable,
			final List<Column> targetColumns) {
		if ( sourceColumns.size() != targetColumns.size() ) {
			throw bindingContext().makeMappingException(
					String.format(
							"Non-matching number columns in foreign key source columns [%s : %s] and target columns [%s : %s]",
							sourceTable.getLogicalName().getText(),
							sourceColumns.size(),
							targetTable.getLogicalName().getText(),
							targetColumns.size()
					)
			);
		}
		for ( int i = 0; i < sourceColumns.size(); i++ ) {
			foreignKey.addColumnMapping( sourceColumns.get( i ), targetColumns.get( i ) );
		}
	}

	private ForeignKey locateAndBindForeignKeyByName(
			final String foreignKeyName,
			final TableSpecification sourceTable,
			final List<Column> sourceColumns,
			final TableSpecification targetTable,
			final List<Column> targetColumns) {
		if ( foreignKeyName == null ) {
			throw new AssertionFailure( "foreignKeyName must be non-null." );
		}
		ForeignKey foreignKey = sourceTable.locateForeignKey( foreignKeyName );
		if ( foreignKey != null ) {
			if ( !targetTable.equals( foreignKey.getTargetTable() ) ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Unexpected target table defined for foreign key \"%s\"; expected \"%s\"; found \"%s\"",
								foreignKeyName,
								targetTable.getLogicalName(),
								foreignKey.getTargetTable().getLogicalName()
						)
				);
			}
			// check if source and target columns have been bound already
			if ( foreignKey.getColumnSpan() == 0 ) {
				// foreign key was found, but no columns bound to it yet
				bindForeignKeyColumns( foreignKey, sourceTable, sourceColumns, targetTable, targetColumns );
			}
			else {
				// The located foreign key already has columns bound;
				// Make sure they are the same columns.
				if ( !foreignKey.getSourceColumns().equals( sourceColumns ) ||
						!foreignKey.getTargetColumns().equals( targetColumns ) ) {
					throw bindingContext().makeMappingException(
							String.format(
									"Attempt to bind exisitng foreign key \"%s\" with different columns.",
									foreignKeyName
							)
					);
				}
			}
		}
		return foreignKey;
	}

	private BinderLocalBindingContext bindingContext() {
		return helperContext.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext();
	}

	private class JoinColumnResolutionContextImpl implements ForeignKeyContributingSource.JoinColumnResolutionContext {
		private final EntityBinding referencedEntityBinding;


		public JoinColumnResolutionContextImpl(EntityBinding referencedEntityBinding) {
			this.referencedEntityBinding = referencedEntityBinding;
		}

		@Override
		public Column resolveColumn(
				String logicalColumnName,
				String logicalTableName,
				String logicalSchemaName,
				String logicalCatalogName) {
			if ( bindingContext().quoteIdentifiersInContext()
					&& !StringHelper.isQuoted( logicalColumnName ) ) {
				logicalColumnName = StringHelper.quote( logicalColumnName );
			}
			return resolveTable( logicalTableName, logicalSchemaName, logicalCatalogName )
					.locateOrCreateColumn( logicalColumnName );
		}

		@Override
		public TableSpecification resolveTable(String logicalTableName, String logicalSchemaName, String logicalCatalogName) {
			Identifier tableIdentifier = helperContext.relationalIdentifierHelper().createIdentifier( logicalTableName );
			if ( tableIdentifier == null ) {
				// todo : why not just return referencedEntityBinding.getPrimaryTable() here?
				//  		is it really valid to expect the table name to be missing, but the
				// 			schema/catalog to be specified as an indication to look for a table
				// 			with the same name as the primary table, but in the specified
				//			schema/catalog?
				tableIdentifier = referencedEntityBinding.getPrimaryTable().getLogicalName();
			}

			final Identifier catalogName = StringHelper.isNotEmpty( logicalCatalogName )
					? Identifier.toIdentifier( logicalCatalogName )
					: referencedEntityBinding.getPrimaryTable().getSchema().getName().getCatalog();
			final Identifier schemaName = StringHelper.isNotEmpty( logicalCatalogName )
					? Identifier.toIdentifier( logicalSchemaName )
					: referencedEntityBinding.getPrimaryTable().getSchema().getName().getSchema();
			final Schema schema = bindingContext().getMetadataCollector().getDatabase()
					.getSchema( catalogName, schemaName );
			return schema.locateTable( tableIdentifier );
		}

		@Override
		public List<Value> resolveRelationalValuesForAttribute(String attributeName) {
			if ( attributeName == null ) {
				List<Value> values = new ArrayList<Value>();
				for ( Column column : referencedEntityBinding.getPrimaryTable().getPrimaryKey().getColumns() ) {
					values.add( column );
				}
				return values;
			}
			List<RelationalValueBinding> valueBindings =
					resolveReferencedAttributeBinding( attributeName ).getRelationalValueBindings();
			List<Value> values = new ArrayList<Value>( valueBindings.size() );
			for ( RelationalValueBinding valueBinding : valueBindings ) {
				values.add( valueBinding.getValue() );
			}
			return values;
		}

		@Override
		public TableSpecification resolveTableForAttribute(String attributeName) {
			if ( attributeName == null ) {
				return referencedEntityBinding.getPrimaryTable();
			}
			else {
				return resolveReferencedAttributeBinding( attributeName ).getRelationalValueBindings().get( 0 ).getTable();
			}
		}

		private SingularAttributeBinding resolveReferencedAttributeBinding(String attributeName) {
			if ( attributeName == null ) {
				return referencedEntityBinding.getHierarchyDetails()
						.getEntityIdentifier()
						.getEntityIdentifierBinding()
						.getAttributeBinding();
			}
			final AttributeBinding referencedAttributeBinding =
					referencedEntityBinding.locateAttributeBindingByPath( attributeName, true );
			if ( referencedAttributeBinding == null ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Could not resolve named referenced property [%s] against entity [%s]",
								attributeName,
								referencedEntityBinding.getEntityName()
						)
				);
			}
			if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Referenced property [%s] against entity [%s] is a plural attribute; it must be a singular attribute.",
								attributeName,
								referencedEntityBinding.getEntityName()
						)
				);
			}
			return (SingularAttributeBinding) referencedAttributeBinding;
		}
	}
}
