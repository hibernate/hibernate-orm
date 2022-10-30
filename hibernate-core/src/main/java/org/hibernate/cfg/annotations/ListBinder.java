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
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AnnotatedColumn;
import org.hibernate.cfg.AnnotatedColumns;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyHolderBuilder;
import org.hibernate.cfg.SecondPass;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.List;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.StringHelper.qualify;

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
	public SecondPass getSecondPass() {
		return new CollectionSecondPass( ListBinder.this.collection ) {
			@Override
            public void secondPass(Map<String, PersistentClass> persistentClasses)
					throws MappingException {
				bindStarToManySecondPass( persistentClasses );
				bindIndex();
			}
		};
	}

	private void bindIndex() {
		final PropertyHolder valueHolder = PropertyHolderBuilder.buildPropertyHolder(
				collection,
				qualify( collection.getRole(), "key" ),
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
		final AnnotatedColumns result = new AnnotatedColumns();
		result.setColumns( new AnnotatedColumn[] { indexColumn } );
		valueBinder.setColumns(result);
		valueBinder.setReturnedClassName( Integer.class.getName() );
		valueBinder.setType( property, getElementType(), null, null );
//			valueBinder.setExplicitType( "integer" );
		final SimpleValue indexValue = valueBinder.make();
		indexColumn.linkWithValue( indexValue );
		listValueMapping.setIndex( indexValue );
		listValueMapping.setBaseIndex( indexColumn.getBase() );
		createBackref( listValueMapping );
	}

	private void createBackref(List listValueMapping) {
		if ( listValueMapping.isOneToMany()
				&& !listValueMapping.getKey().isNullable()
				&& !listValueMapping.isInverse() ) {
			final String entityName = ( (OneToMany) listValueMapping.getElement() ).getReferencedEntityName();
			final PersistentClass referenced = buildingContext.getMetadataCollector().getEntityBinding( entityName );
			final IndexBackref backref = new IndexBackref();
			backref.setName( '_' + propertyName + "IndexBackref" );
			backref.setUpdateable( false );
			backref.setSelectable( false );
			backref.setCollectionRole( listValueMapping.getRole() );
			backref.setEntityName( listValueMapping.getOwner().getEntityName() );
			backref.setValue( listValueMapping.getIndex() );
			referenced.addProperty( backref );
		}
	}
}
