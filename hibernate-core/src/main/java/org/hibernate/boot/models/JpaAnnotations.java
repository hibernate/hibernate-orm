/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import org.hibernate.boot.models.categorize.internal.OrmAnnotationHelper;
import org.hibernate.models.spi.AnnotationDescriptor;

import jakarta.persistence.Access;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Converts;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EntityResult;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.FieldResult;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.TableGenerators;
import jakarta.persistence.Temporal;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import static org.hibernate.models.internal.AnnotationHelper.createOrmDescriptor;

/**
 * Descriptors for JPA annotations
 *
 * @author Steve Ebersole
 */
public interface JpaAnnotations {
	AnnotationDescriptor<Access> ACCESS = createOrmDescriptor( Access.class );
	AnnotationDescriptor<AssociationOverrides> ASSOCIATION_OVERRIDES = createOrmDescriptor( AssociationOverrides.class );
	AnnotationDescriptor<AssociationOverride> ASSOCIATION_OVERRIDE = createOrmDescriptor( AssociationOverride.class, ASSOCIATION_OVERRIDES );
	AnnotationDescriptor<AttributeOverrides> ATTRIBUTE_OVERRIDES = createOrmDescriptor( AttributeOverrides.class );
	AnnotationDescriptor<AttributeOverride> ATTRIBUTE_OVERRIDE = createOrmDescriptor( AttributeOverride.class, ATTRIBUTE_OVERRIDES );
	AnnotationDescriptor<Basic> BASIC = createOrmDescriptor( Basic.class );
	AnnotationDescriptor<Cacheable> CACHEABLE = createOrmDescriptor( Cacheable.class );
	AnnotationDescriptor<CollectionTable> COLLECTION_TABLE = createOrmDescriptor( CollectionTable.class );
	AnnotationDescriptor<Column> COLUMN = createOrmDescriptor( Column.class );
	AnnotationDescriptor<ColumnResult> COLUMN_RESULT = createOrmDescriptor( ColumnResult.class );
	AnnotationDescriptor<ConstructorResult> CONSTRUCTOR_RESULT = createOrmDescriptor( ConstructorResult.class );
	AnnotationDescriptor<Converts> CONVERTS = createOrmDescriptor( Converts.class );
	AnnotationDescriptor<Convert> CONVERT = createOrmDescriptor( Convert.class, CONVERTS );
	AnnotationDescriptor<Converter> CONVERTER = createOrmDescriptor( Converter.class );
	AnnotationDescriptor<DiscriminatorColumn> DISCRIMINATOR_COLUMN = createOrmDescriptor( DiscriminatorColumn.class );
	AnnotationDescriptor<DiscriminatorValue> DISCRIMINATOR_VALUE = createOrmDescriptor( DiscriminatorValue.class );
	AnnotationDescriptor<ElementCollection> ELEMENT_COLLECTION = createOrmDescriptor( ElementCollection.class );
	AnnotationDescriptor<Embeddable> EMBEDDABLE = createOrmDescriptor( Embeddable.class );
	AnnotationDescriptor<Embedded> EMBEDDED = createOrmDescriptor( Embedded.class );
	AnnotationDescriptor<EmbeddedId> EMBEDDED_ID = createOrmDescriptor( EmbeddedId.class );
	AnnotationDescriptor<Entity> ENTITY = createOrmDescriptor( Entity.class );
	AnnotationDescriptor<EntityListeners> ENTITY_LISTENERS = createOrmDescriptor( EntityListeners.class );
	AnnotationDescriptor<EntityResult> ENTITY_RESULT = createOrmDescriptor( EntityResult.class );
	AnnotationDescriptor<Enumerated> ENUMERATED = createOrmDescriptor( Enumerated.class );
	AnnotationDescriptor<ExcludeDefaultListeners> EXCLUDE_DEFAULT_LISTENERS = createOrmDescriptor( ExcludeDefaultListeners.class );
	AnnotationDescriptor<ExcludeSuperclassListeners> EXCLUDE_SUPERCLASS_LISTENERS = createOrmDescriptor( ExcludeSuperclassListeners.class );
	AnnotationDescriptor<FieldResult> FIELD_RESULT = createOrmDescriptor( FieldResult.class );
	AnnotationDescriptor<ForeignKey> FOREIGN_KEY = createOrmDescriptor( ForeignKey.class );
	AnnotationDescriptor<GeneratedValue> GENERATED_VALUE = createOrmDescriptor( GeneratedValue.class );
	AnnotationDescriptor<Id> ID = createOrmDescriptor( Id.class );
	AnnotationDescriptor<IdClass> ID_CLASS = createOrmDescriptor( IdClass.class );
	AnnotationDescriptor<Index> INDEX = createOrmDescriptor( Index.class );
	AnnotationDescriptor<Inheritance> INHERITANCE = createOrmDescriptor( Inheritance.class );
	AnnotationDescriptor<JoinColumns> JOIN_COLUMNS = createOrmDescriptor( JoinColumns.class );
	AnnotationDescriptor<JoinColumn> JOIN_COLUMN = createOrmDescriptor( JoinColumn.class, JOIN_COLUMNS );
	AnnotationDescriptor<JoinTable> JOIN_TABLE = createOrmDescriptor( JoinTable.class );
	AnnotationDescriptor<Lob> LOB = createOrmDescriptor( Lob.class );
	AnnotationDescriptor<ManyToMany> MANY_TO_MANY = createOrmDescriptor( ManyToMany.class );
	AnnotationDescriptor<ManyToOne> MANY_TO_ONE = createOrmDescriptor( ManyToOne.class );
	AnnotationDescriptor<MapKey> MAP_KEY = createOrmDescriptor( MapKey.class );
	AnnotationDescriptor<MapKeyClass> MAP_KEY_CLASS = createOrmDescriptor( MapKeyClass.class );
	AnnotationDescriptor<MapKeyColumn> MAP_KEY_COLUMN = createOrmDescriptor( MapKeyColumn.class );
	AnnotationDescriptor<MapKeyEnumerated> MAP_KEY_ENUMERATED = createOrmDescriptor( MapKeyEnumerated.class );
	AnnotationDescriptor<MapKeyJoinColumns> MAP_KEY_JOIN_COLUMNS = createOrmDescriptor( MapKeyJoinColumns.class );
	AnnotationDescriptor<MapKeyJoinColumn> MAP_KEY_JOIN_COLUMN = createOrmDescriptor( MapKeyJoinColumn.class, MAP_KEY_JOIN_COLUMNS );
	AnnotationDescriptor<MapKeyTemporal> MAP_KEY_TEMPORAL = createOrmDescriptor( MapKeyTemporal.class );
	AnnotationDescriptor<MappedSuperclass> MAPPED_SUPERCLASS = createOrmDescriptor( MappedSuperclass.class );
	AnnotationDescriptor<MapsId> MAPS_ID = createOrmDescriptor( MapsId.class );
	AnnotationDescriptor<NamedAttributeNode> NAMED_ATTRIBUTE_NODE = createOrmDescriptor( NamedAttributeNode.class );
	AnnotationDescriptor<NamedEntityGraphs> NAMED_ENTITY_GRAPHS = createOrmDescriptor( NamedEntityGraphs.class );
	AnnotationDescriptor<NamedEntityGraph> NAMED_ENTITY_GRAPH = createOrmDescriptor( NamedEntityGraph.class, NAMED_ENTITY_GRAPHS );
	AnnotationDescriptor<NamedNativeQueries> NAMED_NATIVE_QUERIES = createOrmDescriptor( NamedNativeQueries.class );
	AnnotationDescriptor<NamedNativeQuery> NAMED_NATIVE_QUERY = createOrmDescriptor( NamedNativeQuery.class, NAMED_NATIVE_QUERIES );
	AnnotationDescriptor<NamedQueries> NAMED_QUERIES = createOrmDescriptor( NamedQueries.class );
	AnnotationDescriptor<NamedQuery> NAMED_QUERY = createOrmDescriptor( NamedQuery.class, NAMED_QUERIES );
	AnnotationDescriptor<NamedStoredProcedureQueries> NAMED_STORED_PROCEDURE_QUERIES = createOrmDescriptor( NamedStoredProcedureQueries.class );
	AnnotationDescriptor<NamedStoredProcedureQuery> NAMED_STORED_PROCEDURE_QUERY = createOrmDescriptor( NamedStoredProcedureQuery.class, NAMED_STORED_PROCEDURE_QUERIES );
	AnnotationDescriptor<NamedSubgraph> NAMED_SUB_GRAPH = createOrmDescriptor( NamedSubgraph.class );
	AnnotationDescriptor<OneToMany> ONE_TO_MANY = createOrmDescriptor( OneToMany.class );
	AnnotationDescriptor<OneToOne> ONE_TO_ONE = createOrmDescriptor( OneToOne.class );
	AnnotationDescriptor<OrderBy> ORDER_BY = createOrmDescriptor( OrderBy.class );
	AnnotationDescriptor<OrderColumn> ORDER_COLUMN = createOrmDescriptor( OrderColumn.class );
	AnnotationDescriptor<PostLoad> POST_LOAD = createOrmDescriptor( PostLoad.class );
	AnnotationDescriptor<PostPersist> POST_PERSIST = createOrmDescriptor( PostPersist.class );
	AnnotationDescriptor<PostRemove> POST_REMOVE = createOrmDescriptor( PostRemove.class );
	AnnotationDescriptor<PostUpdate> POST_UPDATE = createOrmDescriptor( PostUpdate.class );
	AnnotationDescriptor<PrePersist> PRE_PERSIST = createOrmDescriptor( PrePersist.class );
	AnnotationDescriptor<PreRemove> PRE_REMOVE = createOrmDescriptor( PreRemove.class );
	AnnotationDescriptor<PreUpdate> PRE_UPDATE = createOrmDescriptor( PreUpdate.class );
	AnnotationDescriptor<PrimaryKeyJoinColumns> PRIMARY_KEY_JOIN_COLUMNS = createOrmDescriptor( PrimaryKeyJoinColumns.class );
	AnnotationDescriptor<PrimaryKeyJoinColumn> PRIMARY_KEY_JOIN_COLUMN = createOrmDescriptor( PrimaryKeyJoinColumn.class, PRIMARY_KEY_JOIN_COLUMNS );
	AnnotationDescriptor<QueryHint> QUERY_HINT = createOrmDescriptor( QueryHint.class );
	AnnotationDescriptor<SecondaryTables> SECONDARY_TABLES = createOrmDescriptor( SecondaryTables.class );
	AnnotationDescriptor<SecondaryTable> SECONDARY_TABLE = createOrmDescriptor( SecondaryTable.class, SECONDARY_TABLES );
	AnnotationDescriptor<SequenceGenerators> SEQUENCE_GENERATORS = createOrmDescriptor( SequenceGenerators.class );
	AnnotationDescriptor<SequenceGenerator> SEQUENCE_GENERATOR = createOrmDescriptor( SequenceGenerator.class, SEQUENCE_GENERATORS );
	AnnotationDescriptor<SqlResultSetMappings> SQL_RESULT_SET_MAPPINGS = createOrmDescriptor( SqlResultSetMappings.class );
	AnnotationDescriptor<SqlResultSetMapping> SQL_RESULT_SET_MAPPING = createOrmDescriptor( SqlResultSetMapping.class, SQL_RESULT_SET_MAPPINGS );
	AnnotationDescriptor<StoredProcedureParameter> STORED_PROCEDURE_PARAMETER = createOrmDescriptor( StoredProcedureParameter.class );
	AnnotationDescriptor<Table> TABLE = createOrmDescriptor( Table.class );
	AnnotationDescriptor<TableGenerators> TABLE_GENERATORS = createOrmDescriptor( TableGenerators.class );
	AnnotationDescriptor<TableGenerator> TABLE_GENERATOR = createOrmDescriptor( TableGenerator.class, TABLE_GENERATORS );
	AnnotationDescriptor<Temporal> TEMPORAL = createOrmDescriptor( Temporal.class );
	AnnotationDescriptor<Transient> TRANSIENT = createOrmDescriptor( Transient.class );
	AnnotationDescriptor<UniqueConstraint> UNIQUE_CONSTRAINT = createOrmDescriptor( UniqueConstraint.class );
	AnnotationDescriptor<Version> VERSION = createOrmDescriptor( Version.class );

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( JpaAnnotations.class, consumer );
	}
}
