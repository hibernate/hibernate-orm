/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryCollectionReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.procedure.UnknownSqlResultSetMappingException;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.select.SqlSelectable;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.convert.results.internal.ReturnDynamicInstantiationImpl;
import org.hibernate.sql.convert.results.internal.ReturnEntityImpl;
import org.hibernate.sql.convert.results.internal.ReturnScalarImpl;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.exec.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.exec.results.internal.instantiation.ReturnAssemblerConstructorImpl;
import org.hibernate.sql.exec.results.internal.instantiation.ReturnAssemblerListImpl;
import org.hibernate.sql.exec.results.process.internal.SqlSelectionImpl;
import org.hibernate.sql.exec.results.process.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionReader;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sqm.query.expression.Compatibility;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * Utilities used to implement procedure call support.
 *
 * @author Steve Ebersole
 */
public class Util {
	private static final Logger log = Logger.getLogger( Util.class );

	private Util() {
	}

	/**
	 * Makes a copy of the given query return array.
	 *
	 * @param queryReturns The returns to copy
	 *
	 * @return The copy
	 */
	public static NativeSQLQueryReturn[] copy(NativeSQLQueryReturn[] queryReturns) {
		if ( queryReturns == null ) {
			return new NativeSQLQueryReturn[0];
		}

		final NativeSQLQueryReturn[] copy = new NativeSQLQueryReturn[ queryReturns.length ];
		System.arraycopy( queryReturns, 0, copy, 0, queryReturns.length );
		return copy;
	}

	/**
	 * Make a (shallow) copy of query spaces to be synchronized
	 *
	 * @param synchronizedQuerySpaces The query spaces
	 *
	 * @return The copy
	 */
	public static Set<String> copy(Set<String> synchronizedQuerySpaces) {
		return CollectionHelper.makeCopy( synchronizedQuerySpaces );
	}

	/**
	 * Make a (shallow) copy of the JPA query hints map
	 *
	 * @param hints The JPA query hints to copy
	 *
	 * @return The copy
	 */
	public static Map<String,Object> copy(Map<String, Object> hints) {
		return CollectionHelper.makeCopy( hints );
	}

	/**
	 * Context for resolving result-set-mapping definitions
	 */
	public static interface ResultSetMappingResolutionContext {
		/**
		 * Access to the SessionFactory
		 *
		 * @return SessionFactory
		 */
		SessionFactoryImplementor getSessionFactory();

		/**
		 * Locate a ResultSetMappingDefinition by name
		 *
		 * @param name The name of the ResultSetMappingDefinition to locate
		 *
		 * @return The ResultSetMappingDefinition
		 */
		ResultSetMappingDefinition findResultSetMapping(String name);

		/**
		 * Callback to add query returns indicated by the result set mapping(s)
		 *
		 * @param queryReturns The query returns
		 */
		void addQueryReturns(Return... queryReturns);

		/**
		 * Callback to add query spaces indicated by the result set mapping(s)
		 *
		 * @param querySpaces The query spaces
		 */
		void addQuerySpaces(String... querySpaces);
	}

	/**
	 * Resolve the given result set mapping names
	 *
	 * @param context The context for the resolution.  See {@link ResultSetMappingResolutionContext}
	 * @param resultSetMappingNames The names of the result-set-mappings to resolve
	 */
	public static void resolveResultSetMappings(
			ResultSetMappingResolutionContext context,
			String... resultSetMappingNames) {
		final QueryReturnResolver resolver = new QueryReturnResolver( context );

		for ( String resultSetMappingName : resultSetMappingNames ) {
			resolver.resolve( resultSetMappingName );
		}
	}

	/**
	 * Context for resolving result-class definitions
	 */
	public interface ResultClassesResolutionContext {

		/**
		 * Access to the SessionFactory
		 *
		 * @return SessionFactory
		 */
		SessionFactoryImplementor getSessionFactory();

		/**
		 * Callback to add query returns indicated by the result set mapping(s)
		 *
		 * @param queryReturns The query returns
		 */
		void addQueryReturns(Return... queryReturns);

		/**
		 * Callback to add query spaces indicated by the result set mapping(s)
		 *
		 * @param querySpaces The query spaces
		 */
		void addQuerySpaces(String... querySpaces);
	}

	/**
	 * Resolve the given result classes
	 *
	 * @param context The context for the resolution.  See {@link ResultSetMappingResolutionContext}
	 * @param resultClasses The Classes to which the results should be mapped
	 */
	public static void resolveResultClasses(
			ResultClassesResolutionContext context,
			Class... resultClasses) {
		int i = 0;
		for ( Class resultClass : resultClasses ) {
			final EntityPersister persister = context.getSessionFactory().getMetamodel().entityPersister( resultClass.getName() );
			context.addQuerySpaces( (String[]) persister.getQuerySpaces() );
			context.addQueryReturns(
					new ReturnEntityImpl(
							null,
							persister,
							null,
							false,
							// todo : SqlSelection map
							null,
							new PropertyPath( resultClass.getName() ),
							null

					)
			);
		}
	}

	private static class QueryReturnResolver {
		private final ResultSetMappingResolutionContext context;
		private int selectablesCount = 0;

		Map<String,SqlSelection> sqlSelectionMap = new HashMap<>();
		Map<String, FetchParent> fetchParentMap = null;

		public QueryReturnResolver(ResultSetMappingResolutionContext context) {
			this.context = context;
		}

		public void resolve(String resultSetMappingName) {
			log.tracef( "Starting attempt to resolve named result-set-mapping : %s", resultSetMappingName );

			final ResultSetMappingDefinition mapping = context.findResultSetMapping( resultSetMappingName );
			if ( mapping == null ) {
				throw new UnknownSqlResultSetMappingException( "Unknown SqlResultSetMapping [" + resultSetMappingName + "]" );
			}

			log.tracef( "Found result-set-mapping : %s", mapping.traceLoggableFormat() );


			// even though we only read from JDBC via positions now, we can still leverage the specified
			//		aliases here as a key to resolve SqlSelections
			//	todo : implement ^^

			for ( NativeSQLQueryReturn nativeQueryReturn : mapping.getQueryReturns() ) {
				if ( nativeQueryReturn instanceof NativeSQLQueryScalarReturn ) {
					final NativeSQLQueryScalarReturn rtn = (NativeSQLQueryScalarReturn) nativeQueryReturn;
					final ReturnScalarImpl scalarReturn = new ReturnScalarImpl(
							null,
							resolveSqlSelection( (BasicType) rtn.getType(), rtn.getColumnAlias() ),
							null,
							rtn.getType()
					);
					context.addQueryReturns( scalarReturn );
				}
				else if ( nativeQueryReturn instanceof NativeSQLQueryConstructorReturn ) {
					final NativeSQLQueryConstructorReturn rtn = (NativeSQLQueryConstructorReturn) nativeQueryReturn;
					final ReturnDynamicInstantiationImpl dynamicInstantiationReturn = new ReturnDynamicInstantiationImpl(
							new DynamicInstantiation( rtn.getTargetClass() ),
							null,
							buildDynamicInstantiationAssembler( rtn )
					);
					context.addQueryReturns( dynamicInstantiationReturn );
				}
				else if ( nativeQueryReturn instanceof NativeSQLQueryCollectionReturn ) {
					final NativeSQLQueryCollectionReturn rtn = (NativeSQLQueryCollectionReturn) nativeQueryReturn;
					final String role = rtn.getOwnerEntityName() + '.' + rtn.getOwnerProperty();
					final CollectionPersister persister = context.getSessionFactory().getMetamodel().collectionPersister( role );
					//context.addQueryReturns( ... );
					throw new NotYetImplementedException( "Collection Returns not yet implemented" );
				}
				else if ( nativeQueryReturn instanceof NativeSQLQueryRootReturn ) {
					final NativeSQLQueryRootReturn rtn = (NativeSQLQueryRootReturn) nativeQueryReturn;
					final EntityPersister persister = context.getSessionFactory().getMetamodel().entityPersister( rtn.getReturnEntityName() );
					final ReturnEntityImpl entityReturn = new ReturnEntityImpl(
							null,
							persister,
							null,
							false,
							// todo : SqlSelections
							null,
							new PropertyPath( rtn.getReturnEntityName() ),
							null
					);
					context.addQueryReturns( entityReturn );
					if ( fetchParentMap == null ) {
						fetchParentMap = new HashMap<>();
					}
					fetchParentMap.put( rtn.getAlias(), entityReturn );
				}
				else if ( nativeQueryReturn instanceof NativeSQLQueryJoinReturn ) {
					final NativeSQLQueryJoinReturn rtn = (NativeSQLQueryJoinReturn) nativeQueryReturn;
					// tod finish
				}
			}
		}

		private SqlSelection resolveSqlSelection(BasicType ormType, String alias) {
			return sqlSelectionMap.computeIfAbsent(
					alias,
					s -> new SqlSelectionImpl(
							new SqlSelectable() {
								@Override
								public SqlSelectionReader getSqlSelectionReader() {
									return new SqlSelectionReaderImpl( ormType );
								}

								@Override
								public void accept(SqlSelectAstToJdbcSelectConverter interpreter) {
									throw new HibernateException( "Unexpected call to SqlSelectable#accept as part of procedure/native query Return" );
								}
							},
							selectablesCount++
					)
			);
		}

		private ReturnAssembler buildDynamicInstantiationAssembler(NativeSQLQueryConstructorReturn nativeQueryReturn) {
			final Class target = nativeQueryReturn.getTargetClass();
			if ( Map.class.equals( target ) ) {
				throw new HibernateException( "Map dynamic-instantiations not allowed for native/procedure queries" );
			}

			final List<ArgumentReader> argumentReaders = new ArrayList<>();

			for ( NativeSQLQueryScalarReturn argument : nativeQueryReturn.getColumnReturns() ) {
				final BasicType ormType = (BasicType) argument.getType();
				final ReturnScalarImpl argumentReturn = new ReturnScalarImpl(
						null,
						resolveSqlSelection( ormType, argument.getColumnAlias() ),
						null,
						ormType
				);
				argumentReaders.add( new ArgumentReader( argumentReturn.getReturnAssembler(), null ) );
			}

			if ( List.class.equals( target ) ) {
				return new ReturnAssemblerListImpl( argumentReaders );
			}
			else {
				// find a constructor matching argument types
				constructor_loop:
				for ( Constructor constructor : target.getDeclaredConstructors() ) {
					if ( constructor.getParameterTypes().length != argumentReaders.size() ) {
						continue;
					}

					for ( int i = 0; i < argumentReaders.size(); i++ ) {
						final ArgumentReader argumentReader = argumentReaders.get( i );
						// todo : move Compatibility from SQM into ORM?  It is only used here
						final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
								constructor.getParameterTypes()[i],
								argumentReader.getReturnedJavaType()
						);
						if ( !assignmentCompatible ) {
							log.debugf(
									"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
									i,
									constructor.getParameterTypes()[i],
									argumentReader.getReturnedJavaType()
							);
							continue constructor_loop;
						}
					}

					constructor.setAccessible( true );
					return new ReturnAssemblerConstructorImpl( constructor, argumentReaders );
				}

				throw new HibernateException(
						"Could not locate appropriate constructor for dynamic instantiation of [" + target.getName() + "]"
				);
			}
		}
	}
}
