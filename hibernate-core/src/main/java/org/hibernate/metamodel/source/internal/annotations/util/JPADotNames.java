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
package org.hibernate.metamodel.source.internal.annotations.util;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Converts;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EntityResult;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.FetchType;
import javax.persistence.FieldResult;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.LockModeType;
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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.NamedSubgraph;
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
import javax.persistence.StoredProcedureParameter;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
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
	DotName ACCESS = DotName.createSimple( Access.class.getName() );
	DotName ACCESS_TYPE = DotName.createSimple( AccessType.class.getName() );
	DotName ASSOCIATION_OVERRIDE = DotName.createSimple( AssociationOverride.class.getName() );
	DotName ASSOCIATION_OVERRIDES = DotName.createSimple( AssociationOverrides.class.getName() );
	DotName ATTRIBUTE_OVERRIDE = DotName.createSimple( AttributeOverride.class.getName() );
	DotName ATTRIBUTE_OVERRIDES = DotName.createSimple( AttributeOverrides.class.getName() );
	DotName BASIC = DotName.createSimple( Basic.class.getName() );
	DotName CACHEABLE = DotName.createSimple( Cacheable.class.getName() );
	DotName CASCADE_TYPE = DotName.createSimple( CascadeType.class.getName() );
	DotName COLLECTION_TABLE = DotName.createSimple( CollectionTable.class.getName() );
	DotName COLUMN = DotName.createSimple( Column.class.getName() );
	DotName COLUMN_RESULT = DotName.createSimple( ColumnResult.class.getName() );
	DotName CONVERT = DotName.createSimple( Convert.class.getName() );
	DotName CONVERTER = DotName.createSimple( Converter.class.getName() );
	DotName CONVERTS = DotName.createSimple( Converts.class.getName() );
	DotName DISCRIMINATOR_COLUMN = DotName.createSimple( DiscriminatorColumn.class.getName() );
	DotName DISCRIMINATOR_TYPE = DotName.createSimple( DiscriminatorType.class.getName() );
	DotName DISCRIMINATOR_VALUE = DotName.createSimple( DiscriminatorValue.class.getName() );
	DotName ELEMENT_COLLECTION = DotName.createSimple( ElementCollection.class.getName() );
	DotName EMBEDDABLE = DotName.createSimple( Embeddable.class.getName() );
	DotName EMBEDDED = DotName.createSimple( Embedded.class.getName() );
	DotName EMBEDDED_ID = DotName.createSimple( EmbeddedId.class.getName() );
	DotName ENTITY = DotName.createSimple( Entity.class.getName() );
	DotName ENTITY_LISTENERS = DotName.createSimple( EntityListeners.class.getName() );
	DotName ENTITY_RESULT = DotName.createSimple( EntityResult.class.getName() );
	DotName ENUMERATED = DotName.createSimple( Enumerated.class.getName() );
	DotName ENUM_TYPE = DotName.createSimple( EnumType.class.getName() );
	DotName EXCLUDE_DEFAULT_LISTENERS = DotName.createSimple( ExcludeDefaultListeners.class.getName() );
	DotName EXCLUDE_SUPERCLASS_LISTENERS = DotName.createSimple( ExcludeSuperclassListeners.class.getName() );
	DotName FETCH_TYPE = DotName.createSimple( FetchType.class.getName() );
	DotName FIELD_RESULT = DotName.createSimple( FieldResult.class.getName() );
	DotName FOREIGNKEY = DotName.createSimple( ForeignKey.class.getName() );
	DotName GENERATION_TYPE = DotName.createSimple( GenerationType.class.getName() );
	DotName GENERATED_VALUE = DotName.createSimple( GeneratedValue.class.getName() );
	DotName ID = DotName.createSimple( Id.class.getName() );
	DotName ID_CLASS = DotName.createSimple( IdClass.class.getName() );
	DotName INDEX = DotName.createSimple( Index.class.getName() );
	DotName INHERITANCE = DotName.createSimple( Inheritance.class.getName() );
	DotName INHERITANCE_TYPE = DotName.createSimple( InheritanceType.class.getName() );
	DotName JOIN_COLUMN = DotName.createSimple( JoinColumn.class.getName() );
	DotName JOIN_COLUMNS = DotName.createSimple( JoinColumns.class.getName() );
	DotName JOIN_TABLE = DotName.createSimple( JoinTable.class.getName() );
	DotName LOB = DotName.createSimple( Lob.class.getName() );
	DotName LOCK_MODE_TYPE = DotName.createSimple( LockModeType.class.getName() );
	DotName MANY_TO_MANY = DotName.createSimple( ManyToMany.class.getName() );
	DotName MANY_TO_ONE = DotName.createSimple( ManyToOne.class.getName() );
	DotName MAP_KEY = DotName.createSimple( MapKey.class.getName() );
	DotName MAP_KEY_CLASS = DotName.createSimple( MapKeyClass.class.getName() );
	DotName MAP_KEY_COLUMN = DotName.createSimple( MapKeyColumn.class.getName() );
	DotName MAP_KEY_ENUMERATED = DotName.createSimple( MapKeyEnumerated.class.getName() );
	DotName MAP_KEY_JOIN_COLUMN = DotName.createSimple( MapKeyJoinColumn.class.getName() );
	DotName MAP_KEY_JOIN_COLUMNS = DotName.createSimple( MapKeyJoinColumns.class.getName() );
	DotName MAP_KEY_TEMPORAL = DotName.createSimple( MapKeyTemporal.class.getName() );
	DotName MAPPED_SUPERCLASS = DotName.createSimple( MappedSuperclass.class.getName() );
	DotName MAPS_ID = DotName.createSimple( MapsId.class.getName() );
	DotName NAMED_ATTRIBUTE_NODE = DotName.createSimple( NamedAttributeNode.class.getName() );
	DotName NAMED_ENTITY_GRAPH = DotName.createSimple( NamedEntityGraph.class.getName() );
	DotName NAMED_ENTITY_GRAPHS = DotName.createSimple( NamedEntityGraphs.class.getName() );
	DotName NAMED_NATIVE_QUERIES = DotName.createSimple( NamedNativeQueries.class.getName() );
	DotName NAMED_NATIVE_QUERY = DotName.createSimple( NamedNativeQuery.class.getName() );
	DotName NAMED_QUERIES = DotName.createSimple( NamedQueries.class.getName() );
	DotName NAMED_QUERY = DotName.createSimple( NamedQuery.class.getName() );
	DotName NAMED_STORED_PROCEDURE_QUERIES = DotName.createSimple( NamedStoredProcedureQueries.class.getName() );
	DotName NAMED_STORED_PROCEDURE_QUERY = DotName.createSimple( NamedStoredProcedureQuery.class.getName() );
	DotName NAMED_SUB_GRAPH = DotName.createSimple( NamedSubgraph.class.getName() );
	DotName ONE_TO_MANY = DotName.createSimple( OneToMany.class.getName() );
	DotName ONE_TO_ONE = DotName.createSimple( OneToOne.class.getName() );
	DotName ORDER_BY = DotName.createSimple( OrderBy.class.getName() );
	DotName ORDER_COLUMN = DotName.createSimple( OrderColumn.class.getName() );
	DotName PERSISTENCE_CONTEXT = DotName.createSimple( PersistenceContext.class.getName() );
	DotName PERSISTENCE_CONTEXTS = DotName.createSimple( PersistenceContexts.class.getName() );
	DotName PERSISTENCE_PROPERTY = DotName.createSimple( PersistenceProperty.class.getName() );
	DotName PERSISTENCE_UNIT = DotName.createSimple( PersistenceUnit.class.getName() );
	DotName PERSISTENCE_UNITS = DotName.createSimple( PersistenceUnits.class.getName() );
	DotName POST_LOAD = DotName.createSimple( PostLoad.class.getName() );
	DotName POST_PERSIST = DotName.createSimple( PostPersist.class.getName() );
	DotName POST_REMOVE = DotName.createSimple( PostRemove.class.getName() );
	DotName POST_UPDATE = DotName.createSimple( PostUpdate.class.getName() );
	DotName PRE_PERSIST = DotName.createSimple( PrePersist.class.getName() );
	DotName PRE_REMOVE = DotName.createSimple( PreRemove.class.getName() );
	DotName PRE_UPDATE = DotName.createSimple( PreUpdate.class.getName() );
	DotName PRIMARY_KEY_JOIN_COLUMN = DotName.createSimple( PrimaryKeyJoinColumn.class.getName() );
	DotName PRIMARY_KEY_JOIN_COLUMNS = DotName.createSimple( PrimaryKeyJoinColumns.class.getName() );
	DotName QUERY_HINT = DotName.createSimple( QueryHint.class.getName() );
	DotName SECONDARY_TABLE = DotName.createSimple( SecondaryTable.class.getName() );
	DotName SECONDARY_TABLES = DotName.createSimple( SecondaryTables.class.getName() );
	DotName SEQUENCE_GENERATOR = DotName.createSimple( SequenceGenerator.class.getName() );
	DotName SQL_RESULT_SET_MAPPING = DotName.createSimple( SqlResultSetMapping.class.getName() );
	DotName SQL_RESULT_SET_MAPPINGS = DotName.createSimple( SqlResultSetMappings.class.getName() );
	DotName STORED_PROCEDURE_PARAMETER = DotName.createSimple( StoredProcedureParameter.class.getName() );
	DotName TABLE = DotName.createSimple( Table.class.getName() );
	DotName TABLE_GENERATOR = DotName.createSimple( TableGenerator.class.getName() );
	DotName TEMPORAL = DotName.createSimple( Temporal.class.getName() );
	DotName TEMPORAL_TYPE = DotName.createSimple( TemporalType.class.getName() );
	DotName TRANSIENT = DotName.createSimple( Transient.class.getName() );
	DotName UNIQUE_CONSTRAINT = DotName.createSimple( UniqueConstraint.class.getName() );
	DotName VERSION = DotName.createSimple( Version.class.getName() );
}


