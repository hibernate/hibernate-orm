package org.hibernate.cfg.annotations;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyHolderBuilder;
import org.hibernate.cfg.SecondPass;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.List;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bind a list to the underlying Hibernate configuration
 *
 * @author Matthew Inger
 * @author Emmanuel Bernard
 */
@SuppressWarnings({"unchecked", "serial"})
public class ListBinder extends CollectionBinder {
	private Logger log = LoggerFactory.getLogger( ListBinder.class );

	public ListBinder() {
	}

	protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.List( persistentClass );
	}

	public void setSqlOrderBy(OrderBy orderByAnn) {
		if ( orderByAnn != null ) log.warn( "@OrderBy not allowed for a indexed collection, annotation ignored." );
	}

	public void setSort(Sort sortAnn) {
		if ( sortAnn != null ) log.warn( "@Sort not allowed for a indexed collection, annotation ignored." );
	}

	@Override
	public SecondPass getSecondPass(
			final Ejb3JoinColumn[] fkJoinColumns, final Ejb3JoinColumn[] keyColumns,
			final Ejb3JoinColumn[] inverseColumns,
			final Ejb3Column[] elementColumns,
			Ejb3Column[] mapKeyColumns, final Ejb3JoinColumn[] mapKeyManyToManyColumns, final boolean isEmbedded,
			final XProperty property, final XClass collType,
			final boolean ignoreNotFound, final boolean unique,
			final TableBinder assocTableBinder, final ExtendedMappings mappings
	) {
		return new CollectionSecondPass( mappings, ListBinder.this.collection ) {
			public void secondPass(Map persistentClasses, Map inheritedMetas)
					throws MappingException {
				bindStarToManySecondPass(
						persistentClasses, collType, fkJoinColumns, keyColumns, inverseColumns, elementColumns,
						isEmbedded, property, unique, assocTableBinder, ignoreNotFound, mappings
				);
				bindIndex( mappings );
			}
		};
	}

	private void bindIndex(final ExtendedMappings mappings) {
		if ( indexColumn.isImplicit() == false ) {
			PropertyHolder valueHolder = PropertyHolderBuilder.buildPropertyHolder(
					this.collection,
					StringHelper.qualify( this.collection.getRole(), "key" ),
					(XClass) null,
					(XProperty) null, propertyHolder, mappings
			);
			List list = (List) this.collection;
			if ( !list.isOneToMany() ) indexColumn.forceNotNull();
			indexColumn.setPropertyHolder( valueHolder );
			SimpleValueBinder value = new SimpleValueBinder();
			value.setColumns( new Ejb3Column[] { indexColumn } );
			value.setExplicitType( "integer" );
			value.setMappings( mappings );
			SimpleValue indexValue = value.make();
			indexColumn.linkWithValue( indexValue );
			list.setIndex( indexValue );
			list.setBaseIndex( indexColumn.getBase() );
			if ( list.isOneToMany() && !list.getKey().isNullable() && !list.isInverse() ) {
				String entityName = ( (OneToMany) list.getElement() ).getReferencedEntityName();
				PersistentClass referenced = mappings.getClass( entityName );
				IndexBackref ib = new IndexBackref();
				ib.setName( '_' + propertyName + "IndexBackref" );
				ib.setUpdateable( false );
				ib.setSelectable( false );
				ib.setCollectionRole( list.getRole() );
				ib.setEntityName( list.getOwner().getEntityName() );
				ib.setValue( list.getIndex() );
				referenced.addProperty( ib );
			}
		}
		else {
			Collection coll = this.collection;
			throw new AnnotationException(
					"List/array has to be annotated with an @IndexColumn: "
							+ coll.getRole()
			);
		}
	}
}
