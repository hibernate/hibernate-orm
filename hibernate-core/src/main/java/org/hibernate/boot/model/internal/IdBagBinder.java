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
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

import jakarta.persistence.Column;

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

		final CollectionId collectionIdAnn = property.getDirectAnnotationUsage( CollectionId.class );
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

		final AnnotatedColumns idColumns = AnnotatedColumn.buildColumnsFromAnnotations(
				new Column[]{collectionIdAnn.column()},
//				null,
				null,
				Nullability.FORCED_NOT_NULL,
				propertyHolder,
				propertyData,
				Collections.emptyMap(),
				buildingContext
		);

		//we need to make sure all id columns must be not-null.
		for ( AnnotatedColumn idColumn : idColumns.getColumns() ) {
			idColumn.setNullable( false );
		}

		final BasicValueBinder valueBinder =
				new BasicValueBinder( BasicValueBinder.Kind.COLLECTION_ID, buildingContext );

		final Table table = collection.getCollectionTable();
		valueBinder.setTable( table );
		valueBinder.setColumns( idColumns );

		valueBinder.setType(
				property,
				getElementType(),
				null,
				null
		);

		final BasicValue id = valueBinder.make();
		( (IdentifierCollection) collection ).setIdentifier( id );

		final String namedGenerator = collectionIdAnn.generator();

		switch (namedGenerator) {
			case "identity": {
				throw new MappingException("IDENTITY generation not supported for @CollectionId");
			}
			case "assigned": {
				throw new MappingException("Assigned generation not supported for @CollectionId");
			}
			case "native": {
				throw new MappingException("Native generation not supported for @CollectionId");
			}
		}

		final String generatorName;
		final String generatorType;

		if ( "sequence".equals( namedGenerator ) ) {
			generatorType = namedGenerator;
			generatorName = "";
		}
		else if ( "increment".equals( namedGenerator ) ) {
			generatorType = namedGenerator;
			generatorName = "";
		}
		else {
			generatorType = namedGenerator;
			generatorName = namedGenerator;
		}

		if ( isGlobalGeneratorNameGlobal( buildingContext ) ) {
			SecondPass secondPass = new IdBagIdGeneratorResolverSecondPass(
					(IdentifierBag) collection,
					id,
					property,
					generatorType,
					generatorName,
					getBuildingContext()
			);
			buildingContext.getMetadataCollector().addSecondPass( secondPass );
		}
		else {
			makeIdGenerator(
					id,
					property,
					generatorType,
					generatorName,
					getBuildingContext(),
					localGenerators
			);
		}
		return result;
	}
}
