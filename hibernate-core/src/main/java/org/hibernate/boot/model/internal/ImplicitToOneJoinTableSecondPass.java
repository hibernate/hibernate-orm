/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

import jakarta.persistence.JoinTable;

import static org.hibernate.boot.model.internal.ToOneBinder.getReferenceEntityName;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotBlank;

/**
 * For {@link jakarta.persistence.ManyToOne} and {@link jakarta.persistence.OneToOne}
 * associations mapped to a {@link JoinTable} with no explicitly-specified
 * {@linkplain JoinTable#name table name}, we need to defer creation of the
 * {@link Table} object.
 *
 * @author Gavin King
 */
public class ImplicitToOneJoinTableSecondPass implements SecondPass {

	private final PropertyHolder propertyHolder;
	private final PropertyData inferredData;
	private final MetadataBuildingContext context;
	private final AnnotatedJoinColumns joinColumns;
	private final JoinTable joinTable;
	private final NotFoundAction notFoundAction;
	private final ManyToOne value;

	public ImplicitToOneJoinTableSecondPass(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MetadataBuildingContext context,
			AnnotatedJoinColumns joinColumns,
			JoinTable joinTable,
			NotFoundAction notFoundAction,
			ManyToOne value) {
		this.propertyHolder = propertyHolder;
		this.inferredData = inferredData;
		this.context = context;
		this.joinColumns = joinColumns;
		this.joinTable = joinTable;
		this.notFoundAction = notFoundAction;
		this.value = value;
	}

	// Note: Instead of deferring creation of the whole Table object, perhaps
	//	     we could create it in the first pass and reset its name here in
	//	     the second pass. The problem is that there is some quite involved
	//	     logic in TableBinder that isn't set up for that.

	private void inferJoinTableName(TableBinder tableBinder, Map<String, PersistentClass> persistentClasses) {
		if ( isEmpty( tableBinder.getName() ) ) {
			final PersistentClass owner = propertyHolder.getPersistentClass();
			final InFlightMetadataCollector collector = context.getMetadataCollector();
			final PersistentClass targetEntity =
					persistentClasses.get( getReferenceEntityName( inferredData, context ) );
			//default value
			tableBinder.setDefaultName(
					owner.getClassName(),
					owner.getEntityName(),
					owner.getJpaEntityName(),
					collector.getLogicalTableName( owner.getTable() ),
					targetEntity != null ? targetEntity.getClassName() : null,
					targetEntity != null ? targetEntity.getEntityName() : null,
					targetEntity != null ? targetEntity.getJpaEntityName() : null,
					targetEntity != null ? collector.getLogicalTableName( targetEntity.getTable() ) : null,
					joinColumns.getPropertyName()
			);
		}
	}

	private TableBinder createTableBinder() {
		final TableBinder tableBinder = new TableBinder();
		tableBinder.setBuildingContext( context );

		final String schema = joinTable.schema();
		if ( isNotBlank( schema ) ) {
			tableBinder.setSchema( schema );
		}

		final String catalog = joinTable.catalog();
		if ( isNotBlank( catalog ) ) {
			tableBinder.setCatalog( catalog );
		}

		final String tableName = joinTable.name();
		if ( isNotBlank( tableName ) ) {
			tableBinder.setName( tableName );
		}

		tableBinder.setUniqueConstraints( joinTable.uniqueConstraints() );
		tableBinder.setJpaIndex( joinTable.indexes() );
		tableBinder.setOptions( joinTable.options() );

		return tableBinder;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
		final TableBinder tableBinder = createTableBinder();
		inferJoinTableName( tableBinder, persistentClasses );
		final Table table = tableBinder.bind();
		value.setTable( table );
		final Join join = propertyHolder.addJoin( joinTable, table, true );
		final PersistentClass owner = propertyHolder.getPersistentClass();
		final Property property = owner.getProperty( inferredData.getPropertyName() );
		assert property != null;
		// move the property from the main table to the new join table
		owner.removeProperty( property );
		join.addProperty( property );
		if ( notFoundAction != null ) {
			join.disableForeignKeyCreation();
		}
		for ( AnnotatedJoinColumn joinColumn : joinColumns.getJoinColumns() ) {
			joinColumn.setExplicitTableName( join.getTable().getName() );
		}
	}
}
