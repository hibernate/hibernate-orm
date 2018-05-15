/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.procedure.UnknownSqlResultSetMappingException;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.ResultSetMappingDescriptor;
import org.hibernate.query.sql.spi.ResolvingSqlSelectionImpl;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.results.internal.domain.entity.EntityResultImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
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
		default SessionFactoryImplementor getSessionFactory() {
			return getSqlAstCreationContext().getSessionFactory();
		}

		SqlAstCreationContext getSqlAstCreationContext();

		/**
		 * Locate a ResultSetMappingDefinition by name
		 *
		 * @param name The name of the ResultSetMappingDefinition to locate
		 *
		 * @return The ResultSetMappingDefinition
		 */
		ResultSetMappingDescriptor findResultSetMapping(String name);

		/**
		 * Callback to add query returns indicated by the result set mapping(s)
		 *
		 * @param queryReturns The query returns
		 */
		void addQueryReturns(DomainResult... queryReturns);

		/**
		 * Callback to add query spaces indicated by the result set mapping(s)
		 *
		 * @param querySpaces The query spaces
		 */
		void addQuerySpaces(String... querySpaces);
	}


	public static void resolveResultSetMapping(
			String resultSetMappingName,
			Consumer<Collection<String>> querySpacesConsumer,
			Consumer<DomainResult> resultConsumer,
			SessionFactoryImplementor sessionFactory) {
		new QueryReturnResolver( sessionFactory, querySpacesConsumer, resultConsumer ).resolve( resultSetMappingName );
	}

	/**
	 * Context for resolving result-class definitions
	 */
	public interface ResultClassesResolutionContext extends AssemblerCreationContext {

		/**
		 * Access to the SessionFactory
		 *
		 * @return SessionFactory
		 */
		SessionFactoryImplementor getSessionFactory();

	}

	@SuppressWarnings("unchecked")
	public static void resolveResultClass(
			Class resultType, Consumer<Collection<String>> querySpacesConsumer,
			Consumer<DomainResult> resultConsumer,
			SessionFactoryImplementor sessionFactory) {
		final JavaTypeDescriptor resultTypeDescriptor = sessionFactory.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( resultType );

		if ( resultTypeDescriptor instanceof BasicJavaDescriptor ) {
			final BasicType basicType = sessionFactory
					.getTypeConfiguration()
					.getBasicTypeRegistry()
					.getBasicType( resultType );

			final SqlExpressableType sqlExpressableType = basicType.getSqlExpressableType(
					sessionFactory.getTypeConfiguration()
			);

			resultConsumer.accept(
					new BasicResultImpl(
							null,
							new ResolvingSqlSelectionImpl( 1, sqlExpressableType ),
							sqlExpressableType
					)
			);
		}
		else if ( resultTypeDescriptor instanceof EntityJavaDescriptor ) {
			final EntityTypeDescriptor entityDescriptor = sessionFactory.getMetamodel().getEntityDescriptor( resultType.getName() );
			querySpacesConsumer.accept( entityDescriptor.getAffectedTableNames() );
			resultConsumer.accept(
					new EntityResultImpl(
							entityDescriptor,
							// todo (6.0) : resultVariable?
							null,
							LockMode.NONE,
							new NavigablePath( entityDescriptor.getEntityName() ),
							// todo (6.0) : creation state & context
							null,
							null
					)
			);
		}

		//		int i = 0;
//		for ( Class resultClass : resultClasses ) {
//			final EntityDescriptor entityDescriptor = context.getSessionFactory().getTypeConfiguration().getEntityDescriptor( resultClass.getName() );
//			context.addQuerySpaces( (String[]) entityDescriptor.getAffectedTableNames() );
//			context.addQueryResult( entityDescriptor.generateQueryResult(  )
//					new QueryResultEntityImpl(
//							entityDescriptor,
//							null,
//							// todo : SqlSelection map
//							null,
//							new NavigablePath( entityDescriptor.getEntityName() ),
//							null
//					)
//			);
//		}

		throw new NotYetImplementedFor6Exception(  );
	}

	private static class QueryReturnResolver {
		private final SessionFactoryImplementor sessionFactory;
		private final Consumer<Collection<String>> querySpacesConsumer;
		private final Consumer<DomainResult> resultConsumer;
		private int selectablesCount = 0;

		Map<String,SqlSelection> sqlSelectionMap = new HashMap<>();
		Map<String, FetchParent> fetchParentMap = null;

		public QueryReturnResolver(
				SessionFactoryImplementor sessionFactory,
				Consumer<Collection<String>> querySpacesConsumer,
				Consumer<DomainResult> resultConsumer) {
			this.sessionFactory = sessionFactory;
			this.querySpacesConsumer = querySpacesConsumer;
			this.resultConsumer = resultConsumer;
		}

		public void resolve(String resultSetMappingName) {
			log.tracef( "Starting attempt to resolve named result-set-mapping : %s", resultSetMappingName );

			final ResultSetMappingDescriptor mapping = sessionFactory.getQueryEngine()
					.getNamedQueryRepository()
					.getResultSetMappingDescriptor( resultSetMappingName );

			if ( mapping == null ) {
				throw new UnknownSqlResultSetMappingException( "Unknown SqlResultSetMapping [" + resultSetMappingName + "]" );
			}

			log.tracef( "Found result-set-mapping : %s", mapping.getName() );


			// even though we only read from JDBC via positions now, we can still leverage the specified
			//		aliases here as a key to resolve SqlSelections
			//	todo : implement ^^

			throw new NotYetImplementedFor6Exception();
		}
//			for ( NativeSQLQueryReturn nativeQueryReturn : mapping.getQueryResultBuilders() ) {
//				if ( nativeQueryReturn instanceof NativeSQLQueryScalarReturn ) {
//					final NativeSQLQueryScalarReturn rtn = (NativeSQLQueryScalarReturn) nativeQueryReturn;
//					final QueryResultScalarImpl scalarReturn = new QueryResultScalarImpl(
//							null,
//							resolveSqlSelection( (BasicType) rtn.getType(), rtn.getColumnAlias() ),
//							null,
//							(BasicType) rtn.getType()
//					);
//					context.addQueryReturns( scalarReturn );
//				}
//				else if ( nativeQueryReturn instanceof NativeSQLQueryConstructorReturn ) {
//					final NativeSQLQueryConstructorReturn rtn = (NativeSQLQueryConstructorReturn) nativeQueryReturn;
//					final QueryResultDynamicInstantiationImpl dynamicInstantiationReturn = new QueryResultDynamicInstantiationImpl(
//							new DynamicInstantiation( rtn.getTargetClass() ),
//							null,
//							buildDynamicInstantiationAssembler( rtn )
//					);
//					context.addQueryReturns( dynamicInstantiationReturn );
//				}
//				else if ( nativeQueryReturn instanceof NativeSQLQueryCollectionReturn ) {
//					final NativeSQLQueryCollectionReturn rtn = (NativeSQLQueryCollectionReturn) nativeQueryReturn;
//					final String role = rtn.getOwnerEntityName() + '.' + rtn.getOwnerProperty();
//					final PersistentCollectionDescriptor persister = context.getSessionFactory().getTypeConfiguration().findCollectionDescriptor( role );
//					//context.addQueryReturns( ... );
//					throw new NotYetImplementedException( "Collection Returns not yet implemented" );
//				}
//				else if ( nativeQueryReturn instanceof NativeSQLQueryRootReturn ) {
//					final NativeSQLQueryRootReturn rtn = (NativeSQLQueryRootReturn) nativeQueryReturn;
//					final EntityDescriptor persister = context.getSessionFactory().getTypeConfiguration().getEntityDescriptor( rtn.getReturnEntityName() );
//					final QueryResultEntityImpl entityReturn = new QueryResultEntityImpl(
//							null,
//							persister,
//							null,
//							// todo : SqlSelections
//							null,
//							new NavigablePath( persister.getEntityName() ),
//							null
//					);
//					context.addQueryReturns( entityReturn );
//					if ( fetchParentMap == null ) {
//						fetchParentMap = new HashMap<>();
//					}
//					fetchParentMap.put( rtn.getAlias(), entityReturn );
//				}
//				else if ( nativeQueryReturn instanceof NativeSQLQueryJoinReturn ) {
//					final NativeSQLQueryJoinReturn rtn = (NativeSQLQueryJoinReturn) nativeQueryReturn;
//					// tod finish
//				}
//			}
//		}
//
//		private SqlSelection resolveSqlSelection(BasicType ormType, String alias) {
//			return sqlSelectionMap.computeIfAbsent(
//					alias,
//					s -> new SqlSelectionImpl(
//							ormType.getSqlSelectionReader()
//							new SqlSelectionReaderImpl( ormType ),
//							selectablesCount++
//					)
//			);
//		}
//
//		private QueryResultAssembler buildDynamicInstantiationAssembler(NativeSQLQueryConstructorReturn nativeQueryReturn) {
//			final JavaTypeDescriptor resultType = context.getSessionFactory()
//					.getTypeConfiguration()
//					.getJavaTypeDescriptorRegistry()
//					.getDescriptor( nativeQueryReturn.getTargetClass() );
//			final Class targetJavaType = resultType.getJavaType();
//
//			if ( Map.class.equals( targetJavaType ) ) {
//				throw new HibernateException( "Map dynamic-instantiations not allowed for native/procedure queries" );
//			}
//
//			final List<ArgumentReader> argumentReaders = new ArrayList<>();
//
//			for ( NativeSQLQueryScalarReturn argument : nativeQueryReturn.getColumnReturns() ) {
//				final BasicType ormType = (BasicType) argument.getType();
//				final ScalarQueryResultImpl argumentReturn = new ScalarQueryResultImpl(
//						null,
//						resolveSqlSelection( ormType, argument.getColumnAlias() ),
//						null,
//						ormType
//				);
//				argumentReaders.add( new ArgumentReader( argumentReturn.createResultAssembler(), null ) );
//			}
//
//			if ( List.class.equals( targetJavaType ) ) {
//				return new DynamicInstantiationListAssemblerImpl( (BasicJavaDescriptor<List>) resultType, argumentReaders );
//			}
//			else {
//				// find a constructor matching argument types
//				constructor_loop:
//				for ( Constructor constructor : targetJavaType.getDeclaredConstructors() ) {
//					if ( constructor.getParameterTypes().length != argumentReaders.size() ) {
//						continue;
//					}
//
//					for ( int i = 0; i < argumentReaders.size(); i++ ) {
//						final ArgumentReader argumentReader = argumentReaders.get( i );
//						// todo : move Compatibility from SQM into ORM?  It is only used here
//						final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
//								resolveJavaTypeDescriptor( constructor.getParameterTypes()[i] ),
//								argumentReader.getJavaTypeDescriptor()
//						);
//						if ( !assignmentCompatible ) {
//							log.debugf(
//									"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
//									i,
//									constructor.getParameterTypes()[i],
//									argumentReader.getJavaTypeDescriptor().getJavaType().getName()
//							);
//							continue constructor_loop;
//						}
//					}
//
//					constructor.setAccessible( true );
//					return new DynamicInstantiationConstructorAssemblerImpl( constructor, resultType, argumentReaders );
//				}
//
//				throw new HibernateException(
//						"Could not locate appropriate constructor for dynamic instantiation of [" + targetJavaType.getName() + "]"
//				);
//			}
//		}

		@SuppressWarnings("unchecked")
		private JavaTypeDescriptor resolveJavaTypeDescriptor(Class javaType) {
			return sessionFactory.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( javaType );
		}
	}
}
