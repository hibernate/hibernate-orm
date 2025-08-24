/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.List;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

import static org.hibernate.boot.model.internal.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * A {@link CollectionBinder} for {@link org.hibernate.collection.spi.PersistentList lists},
 * whose mapping model type is {@link org.hibernate.mapping.List}.
 *
 * @author Matthew Inger
 * @author Emmanuel Bernard
 */
public class ListBinder extends CollectionBinder {
	public ListBinder(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, false, buildingContext );
	}

	private List getList() {
		return (List) collection;
	}

	@Override
	protected Collection createCollection(PersistentClass owner) {
		return new List( getCustomTypeBeanResolver(), owner, getBuildingContext() );
	}

	@Override
	public SecondPass getSecondPass() {
		return new CollectionSecondPass( ListBinder.this.collection ) {
			@Override
			public void secondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
				bindStarToManySecondPass( persistentClasses );
				bindIndex();
			}
		};
	}

	private void bindIndex() {
		final PropertyHolder valueHolder =
				buildPropertyHolder( collection, getPath(), null, null, propertyHolder, buildingContext );

		if ( !collection.isOneToMany() ) {
			indexColumn.forceNotNull();
		}
		indexColumn.getParent().setPropertyHolder( valueHolder );

		final var valueBinder = new BasicValueBinder( BasicValueBinder.Kind.LIST_INDEX, buildingContext );
		valueBinder.setColumns( indexColumn.getParent() );
		valueBinder.setReturnedClassName( Integer.class.getName() );
		valueBinder.setType( property, getElementType(), null, null );
//			valueBinder.setExplicitType( "integer" );
		final SimpleValue indexValue = valueBinder.make();
		indexColumn.linkWithValue( indexValue );

		final List list = getList();
		list.setIndex( indexValue );
		list.setBaseIndex( indexColumn.getBase() );

		createBackref();
	}

	private String getPath() {
		return qualify( collection.getRole(), "key" );
	}

	private void createBackref() {
		if ( collection.isOneToMany()
				&& !collection.getKey().isNullable()
				&& !collection.isInverse() ) {
			final String entityName = ( (OneToMany) collection.getElement() ).getReferencedEntityName();
			final var referenced = buildingContext.getMetadataCollector().getEntityBinding( entityName );
			final var backref = new IndexBackref();
			backref.setName( '_' + propertyName + "IndexBackref" );
			backref.setOptional( true );
			backref.setUpdatable( false );
			backref.setSelectable( false );
			backref.setCollectionRole( collection.getRole() );
			backref.setEntityName( collection.getOwner().getEntityName() );
			List list = getList();
			backref.setValue( list.getIndex() );
			referenced.addProperty( backref );
		}
	}
}
