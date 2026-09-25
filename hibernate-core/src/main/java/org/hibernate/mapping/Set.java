/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.CollectionType;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.MappingContext;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.usertype.UserCollectionType;

/**
 * A mapping model object representing a collection of type {@link java.util.List}.
 * A set has no nullable element columns (unless it is a one-to-many association).
 * It has a primary key consisting of all columns (i.e. key columns + element columns),
 * or a unique key if some element columns are nullable.
 *
 * @author Gavin King
 */
public non-sealed class Set extends Collection {
	/**
	 * Used by hbm.xml binding
	 */
	public Set(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	/**
	 * Used by annotation binding
	 */
	public Set(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass persistentClass, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, persistentClass, buildingContext );
	}

	private Set(Set original) {
		super( original );
	}

	@Override
	public Set copy() {
		return new Set( this );
	}

	public void validate(MappingContext mappingContext) throws MappingException {
		super.validate( mappingContext );
		//for backward compatibility, disable this:
		/*Iterator iter = getElement().getColumnIterator();
		while ( iter.hasNext() ) {
			Column col = (Column) iter.next();
			if ( !col.isNullable() ) {
				return;
			}
		}
		throw new MappingException("set element mappings must have at least one non-nullable column: " + getRole() );*/
	}

	public boolean isSet() {
		return true;
	}

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return new SortedSetType( getRole(), getReferencedPropertyName(), getComparator() );
		}
		else if ( hasOrder() ) {
			return new OrderedSetType( getRole(), getReferencedPropertyName() );
		}
		else {
			return new SetType( getRole(), getReferencedPropertyName() );
		}
	}

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			final var collectionTable = getCollectionTable();
			if ( !collectionTable.hasPrimaryKey()
					&& collectionTable.getUniqueKeys().isEmpty() ) {
				final var softDeleteColumn = getSoftDeleteColumn();
				final boolean useLiveRowUniqueIndex =
						softDeleteColumn != null && getSoftDeleteStrategy() != SoftDeleteType.TIMESTAMP;
				for ( var selectable : getElement().getSelectables() ) {
					if ( selectable instanceof Column column ) {
						try {
							if ( column.isSqlTypeLob( getMetadata() ) ) {
								return;
							}
						}
						catch (MappingException me) {
							// ignore
						}
					}
				}
				if ( useLiveRowUniqueIndex ) {
					createLiveRowUniqueIndex( collectionTable, softDeleteColumn );
				}
				else {
					final boolean useUniqueKey = getElement().isNullable()
							|| softDeleteColumn != null && softDeleteColumn.isNullable();
					final Constraint key;
					if ( useUniqueKey ) {
						final var uniqueKey = new UniqueKey( collectionTable );
						uniqueKey.setNullsNotDistinct( true );
						key = uniqueKey;
					}
					else {
						key = new PrimaryKey( collectionTable );
					}
					key.addColumns( getKey() );
					for ( var selectable : getElement().getSelectables() ) {
						if ( selectable instanceof Column column ) {
							key.addColumn( column );
						}
					}
					if ( softDeleteColumn != null ) {
						key.addColumn( softDeleteColumn );
					}
					key.setName( determineUniqueKeyName( key.getColumns() ) );
					if ( key.getColumnSpan() > getKey().getColumnSpan() ) {
						if ( useUniqueKey ) {
							collectionTable.addUniqueKey( (UniqueKey) key );
						}
						else {
							collectionTable.setPrimaryKey( (PrimaryKey) key );
						}
					}
				}
//				else {
					//for backward compatibility, allow a set with no not-null
					//element columns, using all columns in the row locator SQL
					//TODO: create an implicit not null constraint on all cols?
//				}
			}
		}
//		else {
			//create an index on the key columns??
//		}
	}

	private void createLiveRowUniqueIndex(Table collectionTable, Column softDeleteColumn) {
		final List<Column> keyColumns = new ArrayList<>();
		for ( var selectable : getKey().getSelectables() ) {
			if ( selectable instanceof Column column ) {
				keyColumns.add( column );
			}
		}
		for ( var selectable : getElement().getSelectables() ) {
			if ( selectable instanceof Column column ) {
				keyColumns.add( column );
			}
		}
		keyColumns.add( softDeleteColumn );

		final var index = collectionTable.getOrCreateIndex( determineUniqueKeyName( keyColumns ) );
		index.setUnique( true );
		for ( int i = 0; i < keyColumns.size() - 1; i++ ) {
			index.addColumn( keyColumns.get( i ) );
		}
		index.addColumn( new Formula( liveRowSoftDeleteFormula( softDeleteColumn ) ) );
	}

	private String liveRowSoftDeleteFormula(Column softDeleteColumn) {
		final String nonDeletedLiteral = softDeleteNonDeletedLiteral( (BasicValue) softDeleteColumn.getValue() );
		return "(case when " + softDeleteColumn.getQuotedName( getMetadata().getDatabase().getDialect() )
				+ " = " + nonDeletedLiteral + " then 1 end)";
	}

	private String softDeleteNonDeletedLiteral(BasicValue softDeleteValue) {
		final var resolution = softDeleteValue.resolve();
		@SuppressWarnings("unchecked")
		final var converter = (BasicValueConverter<Boolean, ?>) resolution.getValueConverter();
		final Object nonDeletedValue = converter == null
				? getSoftDeleteStrategy() == SoftDeleteType.ACTIVE
						? true
						: false
				: converter.toRelationalValue( false );
		@SuppressWarnings("unchecked")
		final var literalFormatter =
				(JdbcLiteralFormatter<Object>) resolution.getJdbcMapping().getJdbcLiteralFormatter();
		return literalFormatter.toJdbcLiteral( nonDeletedValue, getMetadata().getDatabase().getDialect(), null );
	}

	private String determineUniqueKeyName(List<Column> columns) {
		return getBuildingContext().getBuildingOptions().getImplicitNamingStrategy()
				.determineUniqueKeyName( new ImplicitUniqueKeyNameSource() {
					@Override
					public Identifier getTableName() {
						return getTable().getNameIdentifier();
					}

					@Override
					public List<Identifier> getColumnNames() {
						final List<Identifier> list = new ArrayList<>();
						for ( var column : columns ) {
							list.add( column.getNameIdentifier( getBuildingContext() ) );
						}
						return list;
					}

					@Override
					public Identifier getUserProvidedIdentifier() {
						return null;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return Set.this.getBuildingContext();
					}
				} )
				.render( getMetadata().getDatabase().getDialect() );
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
