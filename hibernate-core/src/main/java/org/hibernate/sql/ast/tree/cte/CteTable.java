/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.cte;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.tuple.internal.CteTupleTableGroupProducer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Describes the table definition for the CTE - its name amd its columns
 *
 * @author Steve Ebersole
 */
public class CteTable {
	public static final String ENTITY_ROW_NUMBER_COLUMN = "rn_";

	private final String cteName;
	private final AnonymousTupleTableGroupProducer tableGroupProducer;
	private final List<CteColumn> cteColumns;

	public CteTable(String cteName, List<CteColumn> cteColumns) {
		this( cteName, null, cteColumns );
	}

	public CteTable(String cteName, CteTupleTableGroupProducer tableGroupProducer) {
		this( cteName, tableGroupProducer, tableGroupProducer.determineCteColumns() );
	}

	private CteTable(String cteName, AnonymousTupleTableGroupProducer tableGroupProducer, List<CteColumn> cteColumns) {
		assert cteName != null;
		this.cteName = cteName;
		this.tableGroupProducer = tableGroupProducer;
		this.cteColumns = List.copyOf( cteColumns );
	}

	public String getTableExpression() {
		return cteName;
	}

	public AnonymousTupleTableGroupProducer getTableGroupProducer() {
		return tableGroupProducer;
	}

	public List<CteColumn> getCteColumns() {
		return cteColumns;
	}

	public CteTable withName(String name) {
		return new CteTable( name, tableGroupProducer, cteColumns );
	}

	@Deprecated(forRemoval = true, since = "7.1")
	public static CteTable createIdTable(String cteName, EntityMappingType entityDescriptor) {
		final int numberOfColumns = entityDescriptor.getIdentifierMapping().getJdbcTypeCount();
		final List<CteColumn> columns = new ArrayList<>( numberOfColumns );
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final String idName =
				identifierMapping instanceof SingleAttributeIdentifierMapping
						? identifierMapping.getAttributeName()
						: "id";
		forEachCteColumn( idName, identifierMapping, columns::add );
		return new CteTable( cteName, columns );
	}

	@Deprecated(forRemoval = true, since = "7.1")
	public static CteTable createEntityTable(String cteName, EntityMappingType entityDescriptor) {
		final int numberOfColumns = entityDescriptor.getIdentifierMapping().getJdbcTypeCount();
		final List<CteColumn> columns = new ArrayList<>( numberOfColumns );
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final String idName;
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			idName = identifierMapping.getAttributeName();
		}
		else {
			idName = "id";
		}
		forEachCteColumn( idName, identifierMapping, columns::add );

		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn() && !discriminatorMapping.isFormula() ) {
			forEachCteColumn( "class", discriminatorMapping, columns::add );
		}

		// Collect all columns for all entity subtype attributes
		entityDescriptor.visitSubTypeAttributeMappings(
				attribute -> {
					if ( !( attribute instanceof PluralAttributeMapping ) ) {
						forEachCteColumn( attribute.getAttributeName(), attribute, columns::add );
					}
				}
		);
		// We add a special row number column that we can use to identify and join rows
		columns.add(
				new CteColumn(
						ENTITY_ROW_NUMBER_COLUMN,
						entityDescriptor.getEntityPersister()
								.getFactory()
								.getTypeConfiguration()
								.getBasicTypeForJavaType( Integer.class )
				)
		);
		return new CteTable( cteName, columns );
	}

	public static CteTable createIdTable(String cteName, PersistentClass persistentClass) {
		final Property identifierProperty = persistentClass.getIdentifierProperty();
		final String idName;
		if ( identifierProperty != null ) {
			idName = identifierProperty.getName();
		}
		else {
			idName = "id";
		}
		final List<CteColumn> columns = new ArrayList<>( persistentClass.getIdentifier().getColumnSpan() );
		final Metadata metadata = persistentClass.getIdentifier().getBuildingContext().getMetadataCollector();
		forEachCteColumn( idName, persistentClass.getIdentifier(), columns::add );
		return new CteTable( cteName, columns );
	}

	public static CteTable createEntityTable(String cteName, PersistentClass persistentClass) {
		final List<CteColumn> columns = new ArrayList<>( persistentClass.getTable().getColumnSpan() );
		final Property identifierProperty = persistentClass.getIdentifierProperty();
		final String idName;
		if ( identifierProperty != null ) {
			idName = identifierProperty.getName();
		}
		else {
			idName = "id";
		}
		final Metadata metadata = persistentClass.getIdentifier().getBuildingContext().getMetadataCollector();
		forEachCteColumn( idName, persistentClass.getIdentifier(), columns::add );

		final Value discriminator = persistentClass.getDiscriminator();
		if ( discriminator != null && !discriminator.getSelectables().get( 0 ).isFormula() ) {
			forEachCteColumn( "class", persistentClass.getIdentifier(), columns::add );
		}

		// Collect all columns for all entity subtype attributes
		for ( Property property : persistentClass.getPropertyClosure() ) {
			if ( !property.isSynthetic() ) {
				forEachCteColumn(
						property.getName(),
						property.getValue(),
						columns::add
				);
			}
		}
		// We add a special row number column that we can use to identify and join rows
		columns.add(
				new CteColumn(
						ENTITY_ROW_NUMBER_COLUMN,
						metadata.getDatabase().getTypeConfiguration().getBasicTypeForJavaType( Integer.class )
				)
		);
		return new CteTable( cteName, columns );
	}

	private static void forEachCteColumn(String prefix, Value value, Consumer<CteColumn> consumer) {
		SqmMutationStrategyHelper.forEachSelectableMapping( prefix, value, (columnName, selectable) -> {
			consumer.accept( new CteColumn( columnName, selectable.getType() ) );
		} );
	}

	private static void forEachCteColumn(String prefix, ModelPart modelPart, Consumer<CteColumn> consumer) {
		SqmMutationStrategyHelper.forEachSelectableMapping( prefix, modelPart, (s, selectableMapping) -> {
			consumer.accept( new CteColumn( s, selectableMapping.getJdbcMapping() ) );
		} );
	}

	public List<CteColumn> findCteColumns(ModelPart modelPart) {
		final String prefix;
		if ( modelPart instanceof AttributeMapping attributeMapping ) {
			prefix = attributeMapping.getAttributeName();
		}
		else if ( modelPart instanceof DiscriminatorMapping ) {
			prefix = "class";
		}
		else {
			prefix = "";
		}
		final int jdbcTypeCount = modelPart.getJdbcTypeCount();
		final List<CteColumn> columns = new ArrayList<>( jdbcTypeCount );
		SqmMutationStrategyHelper.forEachSelectableMapping( prefix, modelPart, (s, selectableMapping) -> {
			for ( CteColumn cteColumn : cteColumns ) {
				if ( s.equals( cteColumn.getColumnExpression() ) ) {
					columns.add( cteColumn );
					break;
				}
			}
		} );
		return columns;
	}
}
