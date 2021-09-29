/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.Collections;
import java.util.Map;

import javax.persistence.Column;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Type;
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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

/**
 * @author Emmanuel Bernard
 */
public class IdBagBinder extends BagBinder {
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
		CollectionId collectionIdAnn = property.getAnnotation( CollectionId.class );
		if ( collectionIdAnn != null ) {
			SimpleValueBinder simpleValue = new SimpleValueBinder();

			PropertyData propertyData = new WrappedInferredData(
					new PropertyInferredData(
							null,
							property,
							null, //default access should not be useful
							buildingContext.getBootstrapContext().getReflectionManager()
					),
					"id"
			);
			Ejb3Column[] idColumns = Ejb3Column.buildColumnFromAnnotation(
					determineColumns( collectionIdAnn ),
					null,
					null,
					Nullability.FORCED_NOT_NULL,
					propertyHolder,
					propertyData,
					Collections.EMPTY_MAP,
					buildingContext
			);
			//we need to make sure all id columns must be not-null.
			for(Ejb3Column idColumn:idColumns){
				idColumn.setNullable(false);
			}
			Table table = collection.getCollectionTable();
			simpleValue.setTable( table );
			simpleValue.setColumns( idColumns );
			Type typeAnn = collectionIdAnn.type();
			if ( typeAnn != null && !BinderHelper.isEmptyAnnotationValue( typeAnn.type() ) ) {
				simpleValue.setExplicitType( typeAnn );
			}
			else {
				throw new AnnotationException( "@CollectionId is missing type: "
						+ StringHelper.qualify( propertyHolder.getPath(), propertyName ) );
			}
			simpleValue.setBuildingContext( getBuildingContext() );
			SimpleValue id = simpleValue.make();
			( (IdentifierCollection) collection ).setIdentifier( id );
			String generator = collectionIdAnn.generator();
			String generatorType;
			if ( "identity".equals( generator ) || "assigned".equals( generator )
					|| "sequence".equals( generator ) || "native".equals( generator ) ) {
				generatorType = generator;
				generator = "";
			}
			else {
				generatorType = null;
			}

			if ( buildingContext.getBootstrapContext().getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
				SecondPass secondPass = new IdGeneratorResolverSecondPass(
						id,
						property,
						generatorType,
						generator,
						getBuildingContext()
				);
				buildingContext.getMetadataCollector().addSecondPass( secondPass );
			}
			else {
				BinderHelper.makeIdGenerator(
						id,
						property,
						generatorType,
						generator,
						getBuildingContext(),
						localGenerators
				);
			}
		}
		return result;
	}

	private Column[] determineColumns(CollectionId collectionIdAnn) {
		if ( collectionIdAnn.columns().length > 0 ) {
			return collectionIdAnn.columns();
		}
		final Column column = collectionIdAnn.column();
		if ( StringHelper.isNotEmpty( column.name() ) ) {
			return new Column[] { column };
		}
		// this should mimic the old behavior when `#columns` was not specified
		return new Column[0];
	}
}
