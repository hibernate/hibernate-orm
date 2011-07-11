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
	DotName ACCESS_TYPE = DotName.createSimple( AccessType.class.getName() );
	DotName ANY = DotName.createSimple( Any.class.getName() );
	DotName ANY_META_DEF = DotName.createSimple( AnyMetaDef.class.getName() );
	DotName ANY_META_DEFS = DotName.createSimple( AnyMetaDefs.class.getName() );
	DotName BATCH_SIZE = DotName.createSimple( BatchSize.class.getName() );
	DotName CACHE = DotName.createSimple( Cache.class.getName() );
	DotName CASCADE = DotName.createSimple( Cascade.class.getName() );
	DotName CHECK = DotName.createSimple( Check.class.getName() );
	DotName COLLECTION_ID = DotName.createSimple( CollectionId.class.getName() );
	DotName COLUMNS = DotName.createSimple( Columns.class.getName() );
	DotName COLUMN_TRANSFORMER = DotName.createSimple( ColumnTransformer.class.getName() );
	DotName COLUMN_TRANSFORMERS = DotName.createSimple( ColumnTransformers.class.getName() );
	DotName DISCRIMINATOR_FORMULA = DotName.createSimple( DiscriminatorFormula.class.getName() );
	DotName DISCRIMINATOR_OPTIONS = DotName.createSimple( DiscriminatorOptions.class.getName() );
	DotName ENTITY = DotName.createSimple( Entity.class.getName() );
	DotName FETCH = DotName.createSimple( Fetch.class.getName() );
	DotName FETCH_PROFILE = DotName.createSimple( FetchProfile.class.getName() );
	DotName FETCH_PROFILES = DotName.createSimple( FetchProfiles.class.getName() );
	DotName FILTER = DotName.createSimple( Filter.class.getName() );
	DotName FILTER_DEF = DotName.createSimple( FilterDef.class.getName() );
	DotName FILTER_DEFS = DotName.createSimple( FilterDefs.class.getName() );
	DotName FILTER_JOIN_TABLE = DotName.createSimple( FilterJoinTable.class.getName() );
	DotName FILTER_JOIN_TABLES = DotName.createSimple( FilterJoinTables.class.getName() );
	DotName FILTERS = DotName.createSimple( Filters.class.getName() );
	DotName FOREIGN_KEY = DotName.createSimple( ForeignKey.class.getName() );
	DotName FORMULA = DotName.createSimple( Formula.class.getName() );
	DotName GENERATED = DotName.createSimple( Generated.class.getName() );
	DotName GENERIC_GENERATOR = DotName.createSimple( GenericGenerator.class.getName() );
	DotName GENERIC_GENERATORS = DotName.createSimple( GenericGenerators.class.getName() );
	DotName IMMUTABLE = DotName.createSimple( Immutable.class.getName() );
	DotName INDEX = DotName.createSimple( Index.class.getName() );
	DotName INDEX_COLUMN = DotName.createSimple( IndexColumn.class.getName() );
	DotName JOIN_COLUMN_OR_FORMULA = DotName.createSimple( JoinColumnOrFormula.class.getName() );
	DotName JOIN_COLUMNS_OR_FORMULAS = DotName.createSimple( JoinColumnsOrFormulas.class.getName() );
	DotName JOIN_FORMULA = DotName.createSimple( JoinFormula.class.getName() );
	DotName LAZY_COLLECTION = DotName.createSimple( LazyCollection.class.getName() );
	DotName LAZY_TO_ONE = DotName.createSimple( LazyToOne.class.getName() );
	DotName LOADER = DotName.createSimple( Loader.class.getName() );
	DotName MANY_TO_ANY = DotName.createSimple( ManyToAny.class.getName() );
	DotName MAP_KEY_TYPE = DotName.createSimple( MapKeyType.class.getName() );
	DotName META_VALUE = DotName.createSimple( MetaValue.class.getName() );
	DotName NAMED_NATIVE_QUERIES = DotName.createSimple( NamedNativeQueries.class.getName() );
	DotName NAMED_NATIVE_QUERY = DotName.createSimple( NamedNativeQuery.class.getName() );
	DotName NAMED_QUERIES = DotName.createSimple( NamedQueries.class.getName() );
	DotName NAMED_QUERY = DotName.createSimple( NamedQuery.class.getName() );
	DotName NATURAL_ID = DotName.createSimple( NaturalId.class.getName() );
	DotName NOT_FOUND = DotName.createSimple( NotFound.class.getName() );
	DotName ON_DELETE = DotName.createSimple( OnDelete.class.getName() );
	DotName OPTIMISTIC_LOCK = DotName.createSimple( OptimisticLock.class.getName() );
	DotName ORDER_BY = DotName.createSimple( OrderBy.class.getName() );
	DotName PARAM_DEF = DotName.createSimple( ParamDef.class.getName() );
	DotName PARAMETER = DotName.createSimple( Parameter.class.getName() );
	DotName PARENT = DotName.createSimple( Parent.class.getName() );
	DotName PERSISTER = DotName.createSimple( Persister.class.getName() );
	DotName PROXY = DotName.createSimple( Proxy.class.getName() );
	DotName ROW_ID = DotName.createSimple( RowId.class.getName() );
	DotName SORT = DotName.createSimple( Sort.class.getName() );
	DotName SOURCE = DotName.createSimple( Source.class.getName() );
	DotName SQL_DELETE = DotName.createSimple( SQLDelete.class.getName() );
	DotName SQL_DELETE_ALL = DotName.createSimple( SQLDeleteAll.class.getName() );
	DotName SQL_INSERT = DotName.createSimple( SQLInsert.class.getName() );
	DotName SQL_UPDATE = DotName.createSimple( SQLUpdate.class.getName() );
	DotName SUB_SELECT = DotName.createSimple( Subselect.class.getName() );
	DotName SYNCHRONIZE = DotName.createSimple( Synchronize.class.getName() );
	DotName TABLE = DotName.createSimple( Table.class.getName() );
	DotName TABLES = DotName.createSimple( Tables.class.getName() );
	DotName TARGET = DotName.createSimple( Target.class.getName() );
	DotName TUPLIZER = DotName.createSimple( Tuplizer.class.getName() );
	DotName TUPLIZERS = DotName.createSimple( Tuplizers.class.getName() );
	DotName TYPE = DotName.createSimple( Type.class.getName() );
	DotName TYPE_DEF = DotName.createSimple( TypeDef.class.getName() );
	DotName TYPE_DEFS = DotName.createSimple( TypeDefs.class.getName() );
	DotName WHERE = DotName.createSimple( Where.class.getName() );
	DotName WHERE_JOIN_TABLE = DotName.createSimple( WhereJoinTable.class.getName() );
}


