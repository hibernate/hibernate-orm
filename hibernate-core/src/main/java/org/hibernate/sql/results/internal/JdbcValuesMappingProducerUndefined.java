/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.results.spi.JdbcValuesMapping;
import org.hibernate.sql.results.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.spi.JdbcValuesMetadata;

import org.jboss.logging.Logger;

/**
 * ResultSetMappingDescriptor implementation for cases where we have
 * no mapping info - everything will be discovered
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingProducerUndefined implements JdbcValuesMappingProducer {
	private static final Logger log = Logger.getLogger( JdbcValuesMappingProducerUndefined.class );

	@SuppressWarnings("WeakerAccess")
	public static JdbcValuesMapping resolveStatic(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception( JdbcValuesMappingProducerUndefined.class );
//		final int columnCount = jdbcResultsMetadata.getColumnCount();
//
//		final HashSet<SqlSelection> sqlSelections = new HashSet<>( columnCount );
//		final List<DomainResult> domainResults = CollectionHelper.arrayList( columnCount );
//
//		final TypeConfiguration typeConfiguration = sessionFactory.getMetamodel().getTypeConfiguration();
//
//		for ( int columnPosition = 0; columnPosition < columnCount; columnPosition++ ) {
//			final String columnName = jdbcResultsMetadata.resolveColumnName( columnPosition );
//			log.tracef( "Discovering JDBC result column metadata [%s (%s)]", columnName, columnPosition );
//
//			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( columnPosition );
//			final JavaTypeDescriptor javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeConfiguration );
//
//			log.debugf( "Discovered JDBC result column metadata [%s (%s)] : %s, %s ", columnName, columnPosition, sqlTypeDescriptor, javaTypeDescriptor );
//
//			final SqlSelection sqlSelection = new SqlSelectionImpl(
//					columnPosition,
//					columnName,
//					javaTypeDescriptor,
//					sqlTypeDescriptor,
//					typeConfiguration
//			);
//			sqlSelections.add( sqlSelection );
//
//			domainResults.add(
//					new ResolvedScalarDomainResult(
//							sqlSelection,
//							columnName,
//							javaTypeDescriptor
//					)
//			);
//		}
//
//		return new StandardResultSetMapping( sqlSelections, domainResults );
	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		return resolveStatic( jdbcResultsMetadata, sessionFactory );
	}

//	private static class SqlSelectionImpl implements SqlSelection {
//		private final int valuesArrayPosition;
//		private JdbcValueExtractor jdbcValueExtractor;
//
//		@SuppressWarnings("unchecked")
//		public SqlSelectionImpl(
//				int columnPosition,
//				String columnName,
//				JavaTypeDescriptor javaTypeDescriptor,
//				SqlTypeDescriptor sqlTypeDescriptor,
//				TypeConfiguration typeConfiguration) {
//			log.tracef( "Creating SqlSelection for auto-discovered column : %s (%s)", columnName, columnPosition );
//			this.valuesArrayPosition = columnPosition - 1;
//
//			this.jdbcValueExtractor = sqlTypeDescriptor.getSqlExpressableType( javaTypeDescriptor, typeConfiguration )
//					.getJdbcValueExtractor();
//		}
//
//		@Override
//		public JdbcValueExtractor getJdbcValueExtractor() {
//			return jdbcValueExtractor;
//		}
//
//		@Override
//		public int getValuesArrayPosition() {
//			return valuesArrayPosition;
//		}
//
//		@Override
//		public void accept(SqlAstWalker interpreter) {
//			throw new UnsupportedOperationException();
//		}
//	}
}
