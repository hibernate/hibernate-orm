/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.cfg.AnnotatedColumn;
import org.hibernate.cfg.AnnotatedJoinColumn;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyHolderBuilder;
import org.hibernate.cfg.SecondPass;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.List;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

import org.jboss.logging.Logger;

/**
 * Bind a list to the underlying Hibernate configuration
 *
 * @author Matthew Inger
 * @author Emmanuel Bernard
 */
public class ListBinder extends CollectionBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, ListBinder.class.getName() );

	public ListBinder(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, false, buildingContext );
	}

	@Override
	protected Collection createCollection(PersistentClass owner) {
		return new List( getCustomTypeBeanResolver(), owner, getBuildingContext() );
	}

	@Override
	public void setSqlOrderBy(OrderBy orderByAnn) {
		if ( orderByAnn != null ) {
			LOG.orderByAnnotationIndexedCollection();
		}
	}

	@Override
	public SecondPass getSecondPass(
			final AnnotatedJoinColumn[] fkJoinColumns,
			final AnnotatedJoinColumn[] keyColumns,
			final AnnotatedJoinColumn[] inverseColumns,
			final AnnotatedColumn[] elementColumns,
			AnnotatedColumn[] mapKeyColumns,
			final AnnotatedJoinColumn[] mapKeyManyToManyColumns,
			final boolean isEmbedded,
			final XProperty property,
			final XClass collType,
			final boolean ignoreNotFound,
			final boolean unique,
			final TableBinder assocTableBinder,
			final MetadataBuildingContext buildingContext) {
		return new CollectionSecondPass( getBuildingContext(), ListBinder.this.collection ) {
			@Override
            public void secondPass(Map<String, PersistentClass> persistentClasses)
					throws MappingException {
				bindStarToManySecondPass(
						persistentClasses,
						collType,
						fkJoinColumns,
						keyColumns,
						inverseColumns,
						elementColumns,
						isEmbedded,
						property,
						unique,
						assocTableBinder,
						ignoreNotFound,
						buildingContext
				);
				bindIndex( property, collType, buildingContext );
			}
		};
	}

	private void bindIndex(XProperty property, XClass collType, final MetadataBuildingContext buildingContext) {
		final PropertyHolder valueHolder = PropertyHolderBuilder.buildPropertyHolder(
				collection,
				StringHelper.qualify( collection.getRole(), "key" ),
				null,
				null,
				propertyHolder,
				getBuildingContext()
		);

		final List listValueMapping = (List) collection;

		if ( !listValueMapping.isOneToMany() ) {
			indexColumn.forceNotNull();
		}
		indexColumn.setPropertyHolder( valueHolder );
		final BasicValueBinder valueBinder = new BasicValueBinder( BasicValueBinder.Kind.LIST_INDEX, buildingContext );
		valueBinder.setColumns( new AnnotatedColumn[] { indexColumn } );
		valueBinder.setReturnedClassName( Integer.class.getName() );
		valueBinder.setType( property, collType, null, null );
//			valueBinder.setExplicitType( "integer" );
		SimpleValue indexValue = valueBinder.make();
		indexColumn.linkWithValue( indexValue );
		listValueMapping.setIndex( indexValue );
		listValueMapping.setBaseIndex( indexColumn.getBase() );
		if ( listValueMapping.isOneToMany() && !listValueMapping.getKey().isNullable() && !listValueMapping.isInverse() ) {
			String entityName = ( (OneToMany) listValueMapping.getElement() ).getReferencedEntityName();
			PersistentClass referenced = buildingContext.getMetadataCollector().getEntityBinding( entityName );
			IndexBackref ib = new IndexBackref();
			ib.setName( '_' + propertyName + "IndexBackref" );
			ib.setUpdateable( false );
			ib.setSelectable( false );
			ib.setCollectionRole( listValueMapping.getRole() );
			ib.setEntityName( listValueMapping.getOwner().getEntityName() );
			ib.setValue( listValueMapping.getIndex() );
			referenced.addProperty( ib );
		}
	}
}
