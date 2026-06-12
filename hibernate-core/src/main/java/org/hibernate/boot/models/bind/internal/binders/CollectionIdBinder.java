/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.Map;
import java.util.HashMap;

import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionId;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.Table;

/// Binding for [CollectionId]
///
/// @since 9.0
/// @author Steve Ebersole
class CollectionIdBinder {
	static void bindCollectionId(
			CollectionSource source,
			IdentifierCollection collection,
			Table table,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final CollectionId collectionId = source.member().getDirectAnnotationUsage( CollectionId.class );
		if ( collectionId == null ) {
			throw new MappingException(
					"idbag mapping missing @CollectionId - " + collection.getRole()
			);
		}

		final BasicValue id = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		id.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.collectionId( source.member() ),
				null,
				id,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final org.hibernate.mapping.Column idColumn = ColumnBinder.bindColumn(
				ColumnSource.from( collectionId.column() ),
				() -> IdentifierCollection.DEFAULT_IDENTIFIER_COLUMN_NAME,
				false,
				false
		);
		table.addColumn( idColumn );
		id.addColumn( idColumn );
		collection.setIdentifier( id );

		bindGenerator( collectionId, id, source, bindingState );
	}

	private static void bindGenerator(
			CollectionId collectionId,
			BasicValue id,
			CollectionSource source,
			BindingState bindingState) {
		final Class<? extends IdentifierGenerator> generatorImplementation = collectionId.generatorImplementation();
		final String generator = collectionId.generator();
		if ( generatorImplementation != IdentifierGenerator.class && !generator.isEmpty() ) {
			throw new MappingException(
					"@CollectionId#generatorImplementation and @CollectionId#generator are mutually exclusive - "
							+ source.member()
			);
		}

		checkLegalCollectionIdStrategy( generator );
		GeneratorBinder.makeIdGenerator(
				id,
				source.member(),
				generatorImplementation == IdentifierGenerator.class ? generator : generatorImplementation.getName(),
				generatorName( generator ),
				bindingState.getMetadataBuildingContext(),
				localGenerators( source, bindingState )
		);
	}

	private static Map<String, IdentifierGeneratorDefinition> localGenerators(
			CollectionSource source,
			BindingState bindingState) {
		final HashMap<String, IdentifierGeneratorDefinition> localGenerators = new HashMap<>();
		GeneratorBinder.visitIdGeneratorDefinitions(
				source.member(),
				generator -> localGenerators.put( generator.getName(), generator ),
				bindingState.getMetadataBuildingContext()
		);
		return localGenerators;
	}

	private static String generatorName(String generator) {
		return switch ( generator ) {
			case "sequence", "increment" -> "";
			default -> generator;
		};
	}

	private static void checkLegalCollectionIdStrategy(String namedGenerator) {
		switch ( namedGenerator ) {
			case "identity" -> throw new MappingException( "IDENTITY generation not supported for @CollectionId" );
			case "assigned" -> throw new MappingException( "Assigned generation not supported for @CollectionId" );
			case "native" -> throw new MappingException( "Native generation not supported for @CollectionId" );
			default -> {
			}
		}
	}

	private CollectionIdBinder() {
	}
}
