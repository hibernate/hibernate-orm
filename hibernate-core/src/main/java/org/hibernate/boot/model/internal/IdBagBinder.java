/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionId;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

import jakarta.persistence.Column;

import static org.hibernate.boot.model.internal.BasicValueBinder.Kind.COLLECTION_ID;
import static org.hibernate.boot.model.internal.BinderHelper.isGlobalGeneratorNameGlobal;
import static org.hibernate.boot.model.internal.GeneratorBinder.makeIdGenerator;

/**
 * A {@link CollectionBinder} for {@link org.hibernate.collection.spi.PersistentIdentifierBag id bags}
 * whose mapping model type is {@link org.hibernate.mapping.IdentifierBag}.
 *
 * @author Emmanuel Bernard
 */
public class IdBagBinder extends BagBinder {

	public IdBagBinder(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, buildingContext );
	}

	@Override
	protected Collection createCollection(PersistentClass owner) {
		return new IdentifierBag( getCustomTypeBeanResolver(), owner, getBuildingContext() );
	}

	@Override
	protected boolean bindStarToManySecondPass(Map<String, PersistentClass> persistentClasses) {
		boolean result = super.bindStarToManySecondPass( persistentClasses );

		final var collectionIdAnn = property.getDirectAnnotationUsage( CollectionId.class );
		if ( collectionIdAnn == null ) {
			throw new MappingException( "idbag mapping missing '@CollectionId' annotation" );
		}

		final PropertyData propertyData = new WrappedInferredData(
				new PropertyInferredData(
						null,
						declaringClass,
						property,
						//default access should not be useful
						null,
						buildingContext
				),
				"id"
		);

		final var idColumns = AnnotatedColumn.buildColumnsFromAnnotations(
				new Column[]{collectionIdAnn.column()},
//				null,
				null,
				Nullability.FORCED_NOT_NULL,
				propertyHolder,
				propertyData,
				Collections.emptyMap(),
				buildingContext
		);

		// we need to make sure all id columns must be not-null.
		for ( var idColumn : idColumns.getColumns() ) {
			idColumn.setNullable( false );
		}

		final var idValueBinder = new BasicValueBinder( COLLECTION_ID, buildingContext );
		idValueBinder.setTable( collection.getCollectionTable() );
		idValueBinder.setColumns( idColumns );
		idValueBinder.setType( property, getElementType() );
		final BasicValue id = idValueBinder.make();
		( (IdentifierCollection) collection ).setIdentifier( id );

		final String generator = collectionIdAnn.generator();
		checkLegalCollectionIdStrategy( generator );
		if ( isGlobalGeneratorNameGlobal( buildingContext ) ) {
			buildingContext.getMetadataCollector()
					.addSecondPass( new IdBagIdGeneratorResolverSecondPass(
							id,
							property,
							generator,
							generatorName( generator ),
							getBuildingContext()
					) );
		}
		else {
			makeIdGenerator(
					id,
					property,
					generator,
					generatorName( generator ),
					getBuildingContext(),
					localGenerators
			);
		}
		return result;
	}

	private static String generatorName(String generator) {
		return switch ( generator ) {
			case "sequence", "increment" -> "";
			default -> generator;
		};
	}

	private static void checkLegalCollectionIdStrategy(String namedGenerator) {
		switch ( namedGenerator ) {
			case "identity":
				throw new MappingException("IDENTITY generation not supported for @CollectionId");
			case "assigned":
				throw new MappingException("Assigned generation not supported for @CollectionId");
			case "native":
				throw new MappingException("Native generation not supported for @CollectionId");
		}
	}
}
