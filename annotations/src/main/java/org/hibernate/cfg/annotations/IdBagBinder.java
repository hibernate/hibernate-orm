/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cfg.annotations;

import java.util.Collections;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.PropertyInferredData;
import org.hibernate.cfg.WrappedInferredData;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class IdBagBinder extends BagBinder {
	protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.IdentifierBag( persistentClass );
	}

	@Override
	protected boolean bindStarToManySecondPass(
			Map persistentClasses, XClass collType, Ejb3JoinColumn[] fkJoinColumns, Ejb3JoinColumn[] keyColumns,
			Ejb3JoinColumn[] inverseColumns, Ejb3Column[] elementColumns, boolean isEmbedded, XProperty property,
			boolean unique, TableBinder associationTableBinder, boolean ignoreNotFound, ExtendedMappings mappings
	) {
		boolean result = super.bindStarToManySecondPass(
				persistentClasses, collType, fkJoinColumns, keyColumns, inverseColumns, elementColumns, isEmbedded,
				property, unique, associationTableBinder, ignoreNotFound, mappings
		);
		CollectionId collectionIdAnn = property.getAnnotation( CollectionId.class );
		if ( collectionIdAnn != null ) {
			SimpleValueBinder simpleValue = new SimpleValueBinder();

			PropertyData propertyData = new WrappedInferredData(
					new PropertyInferredData( null, property, null, //default access should not be useful
							mappings.getReflectionManager() ),
					"id" );
			Ejb3Column[] idColumns = Ejb3Column.buildColumnFromAnnotation(
					collectionIdAnn.columns(),
					null,
					Nullability.FORCED_NOT_NULL,
					propertyHolder,
					propertyData,
					Collections.EMPTY_MAP,
					mappings
			);
			Table table = collection.getCollectionTable();
			simpleValue.setTable( table );
			simpleValue.setColumns( idColumns );
			Type typeAnn = collectionIdAnn.type();
			if ( typeAnn != null && !BinderHelper.isDefault( typeAnn.type() ) ) {
				simpleValue.setExplicitType( typeAnn );
			}
			else {
				throw new AnnotationException( "@CollectionId is missing type: "
						+ StringHelper.qualify( propertyHolder.getPath(), propertyName ) );
			}
			simpleValue.setMappings( mappings );
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
			BinderHelper.makeIdGenerator( id, generatorType, generator, mappings, localGenerators );
		}
		return result;
	}
}
