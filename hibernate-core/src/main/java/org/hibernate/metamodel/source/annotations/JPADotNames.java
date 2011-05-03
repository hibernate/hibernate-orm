/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.annotations;

import javax.persistence.Access;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EntityResult;
import javax.persistence.Enumerated;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.FieldResult;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.MapKeyTemporal;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContexts;
import javax.persistence.PersistenceProperty;
import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceUnits;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.QueryHint;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.jboss.jandex.DotName;

/**
 * Defines the dot names for the JPA annotations
 *
 * @author Hardy Ferentschik
 */
public interface JPADotNames {
	public static final DotName ACCESS = DotName.createSimple( Access.class.getName() );
	public static final DotName ASSOCIATION_OVERRIDE = DotName.createSimple( AssociationOverride.class.getName() );
	public static final DotName ASSOCIATION_OVERRIDES = DotName.createSimple( AssociationOverrides.class.getName() );
	public static final DotName ATTRIBUTE_OVERRIDE = DotName.createSimple( AttributeOverride.class.getName() );
	public static final DotName ATTRIBUTE_OVERRIDES = DotName.createSimple( AttributeOverrides.class.getName() );
	public static final DotName BASIC = DotName.createSimple( Basic.class.getName() );
	public static final DotName CACHEABLE = DotName.createSimple( Cacheable.class.getName() );
	public static final DotName COLLECTION_TABLE = DotName.createSimple( CollectionTable.class.getName() );
	public static final DotName COLUMN = DotName.createSimple( Column.class.getName() );
	public static final DotName COLUMN_RESULT = DotName.createSimple( ColumnResult.class.getName() );
	public static final DotName DISCRIMINATOR_COLUMN = DotName.createSimple( DiscriminatorColumn.class.getName() );
	public static final DotName DISCRIMINATOR_VALUE = DotName.createSimple( DiscriminatorValue.class.getName() );
	public static final DotName ELEMENT_COLLECTION = DotName.createSimple( ElementCollection.class.getName() );
	public static final DotName EMBEDDABLE = DotName.createSimple( Embeddable.class.getName() );
	public static final DotName EMBEDDED = DotName.createSimple( Embedded.class.getName() );
	public static final DotName EMBEDDED_ID = DotName.createSimple( EmbeddedId.class.getName() );
	public static final DotName ENTITY = DotName.createSimple( Entity.class.getName() );
	public static final DotName ENTITY_LISTENERS = DotName.createSimple( EntityListeners.class.getName() );
	public static final DotName ENTITY_RESULT = DotName.createSimple( EntityResult.class.getName() );
	public static final DotName ENUMERATED = DotName.createSimple( Enumerated.class.getName() );
	public static final DotName EXCLUDE_DEFAULT_LISTENERS = DotName.createSimple( ExcludeDefaultListeners.class.getName() );
	public static final DotName EXCLUDE_SUPERCLASS_LISTENERS = DotName.createSimple( ExcludeSuperclassListeners.class.getName() );
	public static final DotName FIELD_RESULT = DotName.createSimple( FieldResult.class.getName() );
	public static final DotName GENERATED_VALUE = DotName.createSimple( GeneratedValue.class.getName() );
	public static final DotName ID = DotName.createSimple( Id.class.getName() );
	public static final DotName ID_CLASS = DotName.createSimple( IdClass.class.getName() );
	public static final DotName JOIN_COLUMN = DotName.createSimple( JoinColumn.class.getName() );
	public static final DotName INHERITANCE = DotName.createSimple( Inheritance.class.getName() );
	public static final DotName JOIN_COLUMNS = DotName.createSimple( JoinColumns.class.getName() );
	public static final DotName JOIN_TABLE = DotName.createSimple( JoinTable.class.getName() );
	public static final DotName LOB = DotName.createSimple( Lob.class.getName() );
	public static final DotName MANY_TO_MANY = DotName.createSimple( ManyToMany.class.getName() );
	public static final DotName MANY_TO_ONE = DotName.createSimple( ManyToOne.class.getName() );
	public static final DotName MAP_KEY = DotName.createSimple( MapKey.class.getName() );
	public static final DotName MAP_KEY_CLASS = DotName.createSimple( MapKeyClass.class.getName() );
	public static final DotName MAP_KEY_COLUMN = DotName.createSimple( MapKeyColumn.class.getName() );
	public static final DotName MAP_KEY_ENUMERATED = DotName.createSimple( MapKeyEnumerated.class.getName() );
	public static final DotName MAP_KEY_JOIN_COLUMN = DotName.createSimple( MapKeyJoinColumn.class.getName() );
	public static final DotName MAP_KEY_JOIN_COLUMNS = DotName.createSimple( MapKeyJoinColumns.class.getName() );
	public static final DotName MAP_KEY_TEMPORAL = DotName.createSimple( MapKeyTemporal.class.getName() );
	public static final DotName MAPPED_SUPERCLASS = DotName.createSimple( MappedSuperclass.class.getName() );
	public static final DotName MAPS_ID = DotName.createSimple( MapsId.class.getName() );
	public static final DotName NAMED_NATIVE_QUERIES = DotName.createSimple( NamedNativeQueries.class.getName() );
	public static final DotName NAMED_NATIVE_QUERY = DotName.createSimple( NamedNativeQuery.class.getName() );
	public static final DotName NAMED_QUERIES = DotName.createSimple( NamedQueries.class.getName() );
	public static final DotName NAMED_QUERY = DotName.createSimple( NamedQuery.class.getName() );
	public static final DotName ONE_TO_MANY = DotName.createSimple( OneToMany.class.getName() );
	public static final DotName ONE_TO_ONE = DotName.createSimple( OneToOne.class.getName() );
	public static final DotName ORDER_BY = DotName.createSimple( OrderBy.class.getName() );
	public static final DotName ORDER_COLUMN = DotName.createSimple( OrderColumn.class.getName() );
	public static final DotName PERSISTENCE_CONTEXT = DotName.createSimple( PersistenceContext.class.getName() );
	public static final DotName PERSISTENCE_CONTEXTS = DotName.createSimple( PersistenceContexts.class.getName() );
	public static final DotName PERSISTENCE_PROPERTY = DotName.createSimple( PersistenceProperty.class.getName() );
	public static final DotName PERSISTENCE_UNIT = DotName.createSimple( PersistenceUnit.class.getName() );
	public static final DotName PERSISTENCE_UNITS = DotName.createSimple( PersistenceUnits.class.getName() );
	public static final DotName POST_LOAD = DotName.createSimple( PostLoad.class.getName() );
	public static final DotName POST_PERSIST = DotName.createSimple( PostPersist.class.getName() );
	public static final DotName POST_REMOVE = DotName.createSimple( PostRemove.class.getName() );
	public static final DotName POST_UPDATE = DotName.createSimple( PostUpdate.class.getName() );
	public static final DotName PRE_PERSIST = DotName.createSimple( PrePersist.class.getName() );
	public static final DotName PRE_REMOVE = DotName.createSimple( PreRemove.class.getName() );
	public static final DotName PRE_UPDATE = DotName.createSimple( PreUpdate.class.getName() );
	public static final DotName PRIMARY_KEY_JOIN_COLUMN = DotName.createSimple( PrimaryKeyJoinColumn.class.getName() );
	public static final DotName PRIMARY_KEY_JOIN_COLUMNS = DotName.createSimple( PrimaryKeyJoinColumns.class.getName() );
	public static final DotName QUERY_HINT = DotName.createSimple( QueryHint.class.getName() );
	public static final DotName SECONDARY_TABLE = DotName.createSimple( SecondaryTable.class.getName() );
	public static final DotName SECONDARY_TABLES = DotName.createSimple( SecondaryTables.class.getName() );
	public static final DotName SEQUENCE_GENERATOR = DotName.createSimple( SequenceGenerator.class.getName() );
	public static final DotName SQL_RESULT_SET_MAPPING = DotName.createSimple( SqlResultSetMapping.class.getName() );
	public static final DotName SQL_RESULT_SET_MAPPINGS = DotName.createSimple( SqlResultSetMappings.class.getName() );
	public static final DotName TABLE = DotName.createSimple( Table.class.getName() );
	public static final DotName TABLE_GENERATOR = DotName.createSimple( TableGenerator.class.getName() );
	public static final DotName TEMPORAL = DotName.createSimple( Temporal.class.getName() );
	public static final DotName TRANSIENT = DotName.createSimple( Transient.class.getName() );
	public static final DotName UNIQUE_CONSTRAINT = DotName.createSimple( UniqueConstraint.class.getName() );
	public static final DotName VERSION = DotName.createSimple( Version.class.getName() );
}


