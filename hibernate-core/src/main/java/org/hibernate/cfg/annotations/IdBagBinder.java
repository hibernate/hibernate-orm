/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.Collections;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.IdGeneratorResolverSecondPass;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.PropertyInferredData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.cfg.WrappedInferredData;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

import jakarta.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
public class IdBagBinder extends BagBinder {
	public IdBagBinder() {
	}

	protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.IdentifierBag( getBuildingContext(), persistentClass );
	}

	@Override
	protected boolean bindStarToManySecondPass(
			Map persistentClasses,
			XClass collType,
			Ejb3JoinColumn[] fkJoinColumns,
			Ejb3JoinColumn[] keyColumns,
			Ejb3JoinColumn[] inverseColumns,
			Ejb3Column[] elementColumns,
			boolean isEmbedded,
			XProperty property,
			boolean unique,
			TableBinder associationTableBinder,
			boolean ignoreNotFound,
			MetadataBuildingContext buildingContext) {
		boolean result = super.bindStarToManySecondPass(
				persistentClasses, collType, fkJoinColumns, keyColumns, inverseColumns, elementColumns, isEmbedded,
				property, unique, associationTableBinder, ignoreNotFound, getBuildingContext()
		);

		final CollectionId collectionIdAnn = property.getAnnotation( CollectionId.class );
		if ( collectionIdAnn == null ) {
			throw new MappingException( "idbag mapping missing @CollectionId" );
		}

		final PropertyData propertyData = new WrappedInferredData(
				new PropertyInferredData(
						null,
						property,
						//default access should not be useful
						null,
						buildingContext.getBootstrapContext().getReflectionManager()
				),
				"id"
		);

		final Ejb3Column[] idColumns = Ejb3Column.buildColumnFromAnnotation(
				new Column[] { collectionIdAnn.column() },
				null,
				null,
				Nullability.FORCED_NOT_NULL,
				propertyHolder,
				propertyData,
				Collections.EMPTY_MAP,
				buildingContext
		);

		//we need to make sure all id columns must be not-null.
		for ( Ejb3Column idColumn:idColumns ) {
			idColumn.setNullable( false );
		}

		final BasicValueBinder valueBinder = new BasicValueBinder( BasicValueBinder.Kind.COLLECTION_ID, buildingContext );

		final Table table = collection.getCollectionTable();
		valueBinder.setTable( table );
		valueBinder.setColumns( idColumns );

		valueBinder.setType(
				property,
				collType,
				null,
				null
		);

		final BasicValue id = valueBinder.make();
		( (IdentifierCollection) collection ).setIdentifier( id );

		final String namedGenerator = collectionIdAnn.generator();

		if ( "identity".equals( namedGenerator ) ) {
			throw new MappingException( "IDENTITY generation not supported for CollectionId" );
		}

		if ( "assigned".equals( namedGenerator ) ) {
			throw new MappingException( "Assigned generation not supported for CollectionId" );
		}

		if ( "native".equals( namedGenerator ) ) {
			throw new MappingException( "Native generation not supported for CollectionId" );
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

		id.setIdentifierGeneratorStrategy( generatorType );

		if ( buildingContext.getBootstrapContext().getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
			SecondPass secondPass = new IdGeneratorResolverSecondPass(
					id,
					property,
					generatorType,
					generatorName,
					getBuildingContext()
			);
			buildingContext.getMetadataCollector().addSecondPass( secondPass );
		}
		else {
			BinderHelper.makeIdGenerator(
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
