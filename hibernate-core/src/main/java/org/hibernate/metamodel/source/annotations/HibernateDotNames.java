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

import org.jboss.jandex.DotName;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.AnyMetaDefs;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.ColumnTransformers;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.FilterJoinTables;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ForceDiscriminator;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.MapKeyManyToMany;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.MetaValue;
import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.Source;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.annotations.Table;
import org.hibernate.annotations.Tables;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.Tuplizer;
import org.hibernate.annotations.Tuplizers;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;

/**
 * Defines the dot names for the Hibernate specific mapping annotations.
 *
 * @author Hardy Ferentschik
 */
public interface HibernateDotNames {
	public static final DotName ACCESS_TYPE = DotName.createSimple( AccessType.class.getName() );
	public static final DotName ANY = DotName.createSimple( Any.class.getName() );
	public static final DotName ANY_META_DEF = DotName.createSimple( AnyMetaDef.class.getName() );
	public static final DotName ANY_META_DEFS = DotName.createSimple( AnyMetaDefs.class.getName() );
	public static final DotName BATCH_SIZE = DotName.createSimple( BatchSize.class.getName() );
	public static final DotName CACHE = DotName.createSimple( Cache.class.getName() );
	public static final DotName CASCADE = DotName.createSimple( Cascade.class.getName() );
	public static final DotName CHECK = DotName.createSimple( Check.class.getName() );
	public static final DotName COLLECTION_ID = DotName.createSimple( CollectionId.class.getName() );
	public static final DotName COLLECTION_OF_ELEMENTS = DotName.createSimple( CollectionOfElements.class.getName() );
	public static final DotName COLUMNS = DotName.createSimple( Columns.class.getName() );
	public static final DotName COLUMN_TRANSFORMER = DotName.createSimple( ColumnTransformer.class.getName() );
	public static final DotName COLUMN_TRANSFORMERS = DotName.createSimple( ColumnTransformers.class.getName() );
	public static final DotName DISCRIMINATOR_FORMULA = DotName.createSimple( DiscriminatorFormula.class.getName() );
	public static final DotName DISCRIMINATOR_OPTIONS = DotName.createSimple( DiscriminatorOptions.class.getName() );
	public static final DotName ENTITY = DotName.createSimple( Entity.class.getName() );
	public static final DotName FETCH = DotName.createSimple( Fetch.class.getName() );
	public static final DotName FETCH_PROFILE = DotName.createSimple( FetchProfile.class.getName() );
	public static final DotName FETCH_PROFILES = DotName.createSimple( FetchProfiles.class.getName() );
	public static final DotName FILTER = DotName.createSimple( Filter.class.getName() );
	public static final DotName FILTER_DEF = DotName.createSimple( FilterDef.class.getName() );
	public static final DotName FILTER_DEFS = DotName.createSimple( FilterDefs.class.getName() );
	public static final DotName FILTER_JOIN_TABLE = DotName.createSimple( FilterJoinTable.class.getName() );
	public static final DotName FILTER_JOIN_TABLES = DotName.createSimple( FilterJoinTables.class.getName() );
	public static final DotName FILTERS = DotName.createSimple( Filters.class.getName() );
	public static final DotName FORCE_DISCRIMINATOR = DotName.createSimple( ForceDiscriminator.class.getName() );
	public static final DotName FOREIGN_KEY = DotName.createSimple( ForeignKey.class.getName() );
	public static final DotName FORMULA = DotName.createSimple( Formula.class.getName() );
	public static final DotName GENERATED = DotName.createSimple( Generated.class.getName() );
	public static final DotName GENERIC_GENERATOR = DotName.createSimple( GenericGenerator.class.getName() );
	public static final DotName GENERIC_GENERATORS = DotName.createSimple( GenericGenerators.class.getName() );
	public static final DotName IMMUTABLE = DotName.createSimple( Immutable.class.getName() );
	public static final DotName INDEX = DotName.createSimple( Index.class.getName() );
	public static final DotName INDEX_COLUMN = DotName.createSimple( IndexColumn.class.getName() );
	public static final DotName JOIN_COLUMN_OR_FORMULA = DotName.createSimple( JoinColumnOrFormula.class.getName() );
	public static final DotName JOIN_COLUMNS_OR_FORMULAS = DotName.createSimple( JoinColumnsOrFormulas.class.getName() );
	public static final DotName JOIN_FORMULA = DotName.createSimple( JoinFormula.class.getName() );
	public static final DotName LAZY_COLLECTION = DotName.createSimple( LazyCollection.class.getName() );
	public static final DotName LAZY_TO_ONE = DotName.createSimple( LazyToOne.class.getName() );
	public static final DotName LOADER = DotName.createSimple( Loader.class.getName() );
	public static final DotName MANY_TO_ANY = DotName.createSimple( ManyToAny.class.getName() );
	public static final DotName MAP_KEY = DotName.createSimple( MapKey.class.getName() );
	public static final DotName MAP_KEY_MANY_TO_MANY = DotName.createSimple( MapKeyManyToMany.class.getName() );
	public static final DotName MAP_KEY_TYPE = DotName.createSimple( MapKeyType.class.getName() );
	public static final DotName META_VALUE = DotName.createSimple( MetaValue.class.getName() );
	public static final DotName NAMED_NATIVE_QUERIES = DotName.createSimple( NamedNativeQueries.class.getName() );
	public static final DotName NAMED_NATIVE_QUERY = DotName.createSimple( NamedNativeQuery.class.getName() );
	public static final DotName NAMED_QUERIES = DotName.createSimple( NamedQueries.class.getName() );
	public static final DotName NAMED_QUERY = DotName.createSimple( NamedQuery.class.getName() );
	public static final DotName NATURAL_ID = DotName.createSimple( NaturalId.class.getName() );
	public static final DotName NOT_FOUND = DotName.createSimple( NotFound.class.getName() );
	public static final DotName ON_DELETE = DotName.createSimple( OnDelete.class.getName() );
	public static final DotName OPTIMISTIC_LOCK = DotName.createSimple( OptimisticLock.class.getName() );
	public static final DotName ORDER_BY = DotName.createSimple( OrderBy.class.getName() );
	public static final DotName PARAM_DEF = DotName.createSimple( ParamDef.class.getName() );
	public static final DotName PARAMETER = DotName.createSimple( Parameter.class.getName() );
	public static final DotName PARENT = DotName.createSimple( Parent.class.getName() );
	public static final DotName PERSISTER = DotName.createSimple( Persister.class.getName() );
	public static final DotName PROXY = DotName.createSimple( Proxy.class.getName() );
	public static final DotName ROW_ID = DotName.createSimple( RowId.class.getName() );
	public static final DotName SORT = DotName.createSimple( Sort.class.getName() );
	public static final DotName SOURCE = DotName.createSimple( Source.class.getName() );
	public static final DotName SQL_DELETE = DotName.createSimple( SQLDelete.class.getName() );
	public static final DotName SQL_DELETE_ALL = DotName.createSimple( SQLDeleteAll.class.getName() );
	public static final DotName SQL_INSERT = DotName.createSimple( SQLInsert.class.getName() );
	public static final DotName SQL_UPDATE = DotName.createSimple( SQLUpdate.class.getName() );
	public static final DotName SUB_SELECT = DotName.createSimple( Subselect.class.getName() );
	public static final DotName SYNCHRONIZE = DotName.createSimple( Synchronize.class.getName() );
	public static final DotName TABLE = DotName.createSimple( Table.class.getName() );
	public static final DotName TABLES = DotName.createSimple( Tables.class.getName() );
	public static final DotName TARGET = DotName.createSimple( Target.class.getName() );
	public static final DotName TUPLIZER = DotName.createSimple( Tuplizer.class.getName() );
	public static final DotName TUPLIZERS = DotName.createSimple( Tuplizers.class.getName() );
	public static final DotName TYPE = DotName.createSimple( Type.class.getName() );
	public static final DotName TYPE_DEF = DotName.createSimple( TypeDef.class.getName() );
	public static final DotName TYPE_DEFS = DotName.createSimple( TypeDefs.class.getName() );
	public static final DotName WHERE = DotName.createSimple( Where.class.getName() );
	public static final DotName WHERE_JOIN_TABLE = DotName.createSimple( WhereJoinTable.class.getName() );


}


