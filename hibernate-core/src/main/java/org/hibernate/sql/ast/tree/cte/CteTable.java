/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.tuple.internal.CteTupleTableGroupProducer;

/**
 * Describes the table definition for the CTE - its name amd its columns
 *
 * @author Steve Ebersole
 */
public class CteTable {
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
						"rn_",
						entityDescriptor.getEntityPersister()
								.getFactory()
								.getTypeConfiguration()
								.getBasicTypeForJavaType( Integer.class )
				)
		);
		return new CteTable( cteName, columns );
	}

	public static void forEachCteColumn(String prefix, ModelPart modelPart, Consumer<CteColumn> consumer) {
		SqmMutationStrategyHelper.forEachSelectableMapping( prefix, modelPart, (s, selectableMapping) -> {
			consumer.accept( new CteColumn( s, selectableMapping.getJdbcMapping() ) );
		} );
	}

	public List<CteColumn> findCteColumns(EntityPersister entityDescriptor, ModelPart modelPart) {
		final int offset = determineModelPartStartIndex( entityDescriptor, modelPart );
		if ( offset == -1 ) {
			throw new IllegalStateException( "Couldn't find matching cte columns for: " + modelPart );
		}
		final int end = offset + modelPart.getJdbcTypeCount();
		// Find a matching cte table column and set that at the current index
		return getCteColumns().subList( offset, end );
	}

	private static int determineModelPartStartIndex(EntityPersister entityDescriptor, ModelPart modelPart) {
		int offset = 0;
		final int idResult = determineIdStartIndex( offset, entityDescriptor, modelPart );
		if ( idResult <= 0 ) {
			return -idResult;
		}
		offset = idResult;
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn() && !discriminatorMapping.isFormula() ) {
			if ( modelPart == discriminatorMapping ) {
				return offset;
			}
			offset += discriminatorMapping.getJdbcTypeCount();
		}
		final AttributeMappingsList attributeMappings = entityDescriptor.getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			AttributeMapping attribute = attributeMappings.get( i );
			if ( !( attribute instanceof PluralAttributeMapping ) ) {
				final int result = determineModelPartStartIndex( offset, attribute, modelPart );
				if ( result <= 0 ) {
					return -result;
				}
				offset = result;
			}
		}
		return -1;
	}

	private static int determineIdStartIndex(int offset, EntityPersister entityDescriptor, ModelPart modelPart) {
		final int originalOffset = offset;
		do {
			final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
			final int result = determineModelPartStartIndex( originalOffset, identifierMapping, modelPart );
			offset = result;
			if ( result <= 0 ) {
				break;
			}
			entityDescriptor = (EntityPersister) entityDescriptor.getSuperMappingType();
		} while ( entityDescriptor != null );

		return offset;
	}

	private static int determineModelPartStartIndex(int offset, ModelPart modelPart, ModelPart modelPartToFind) {
		if ( modelPart == modelPartToFind ) {
			return -offset;
		}
		if ( modelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			final ModelPart keyPart =
					modelPart instanceof Association association
							? association.getForeignKeyDescriptor()
							: entityValuedModelPart.getEntityMappingType().getIdentifierMapping();
			return determineModelPartStartIndex( offset, keyPart, modelPartToFind );
		}
		else if ( modelPart instanceof EmbeddableValuedModelPart embeddablePart ) {
			final AttributeMappingsList attributeMappings =
					embeddablePart.getEmbeddableTypeDescriptor().getAttributeMappings();
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping mapping = attributeMappings.get( i );
				final int result = determineModelPartStartIndex( offset, mapping, modelPartToFind );
				if ( result <= 0 ) {
					return result;
				}
				offset = result;
			}
			return offset;
		}
		else if ( modelPart instanceof BasicValuedModelPart basicModelPart ) {
			return offset + (basicModelPart.isInsertable() ? modelPart.getJdbcTypeCount() : 0);
		}
		return offset + modelPart.getJdbcTypeCount();
	}
}
