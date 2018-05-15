/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.HashSet;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.internal.StandardResultSetMapping;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * ResultSetMappingDescriptor implementation for cases where we have
 * no mapping info - everything will be discovered
 *
 * @author Steve Ebersole
 */
public class ResultSetMappingDescriptorUndefined implements ResultSetMappingDescriptor {
	private static final Logger log = Logger.getLogger( ResultSetMappingDescriptorUndefined.class );

	@SuppressWarnings("WeakerAccess")
	public static ResultSetMapping resolveStatic(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		final int columnCount = jdbcResultsMetadata.getColumnCount();

		final HashSet<SqlSelection> sqlSelections = new HashSet<>( columnCount );
		final List<DomainResult> domainResults = CollectionHelper.arrayList( columnCount );

		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

		for ( int columnPosition = 0; columnPosition < columnCount; columnPosition++ ) {
			final String columnName = jdbcResultsMetadata.resolveColumnName( columnPosition );
			log.tracef( "Discovering JDBC result column metadata : %s (%s)", columnName, columnPosition );

			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( columnPosition );
			final BasicJavaDescriptor javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeConfiguration );

			final SqlSelection sqlSelection = new SqlSelectionImpl(
					columnPosition,
					columnName,
					javaTypeDescriptor,
					sqlTypeDescriptor,
					typeConfiguration
			);
			sqlSelections.add( sqlSelection );

			domainResults.add(
					new ResolvedScalarDomainResult(
							sqlSelection,
							columnName,
							javaTypeDescriptor
					)
			);
		}

		return new StandardResultSetMapping( sqlSelections, domainResults );
	}

	@Override
	public ResultSetMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		return resolveStatic( jdbcResultsMetadata, sessionFactory );
	}

	private static class SqlSelectionImpl implements SqlSelection {
		private final int valuesArrayPosition;
		private JdbcValueExtractor jdbcValueExtractor;

		@SuppressWarnings("unchecked")
		public SqlSelectionImpl(
				int columnPosition,
				String columnName,
				BasicJavaDescriptor javaTypeDescriptor,
				SqlTypeDescriptor sqlTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			log.tracef( "Creating SqlSelection for auto-discovered column : %s (%s)", columnName, columnPosition );
			this.valuesArrayPosition = columnPosition - 1;

			this.jdbcValueExtractor = sqlTypeDescriptor.getSqlExpressableType( javaTypeDescriptor, typeConfiguration )
					.getJdbcValueExtractor();
		}

		@Override
		public JdbcValueExtractor getJdbcValueExtractor() {
			return jdbcValueExtractor;
		}

		@Override
		public int getValuesArrayPosition() {
			return valuesArrayPosition;
		}

		@Override
		public void accept(SqlAstWalker interpreter) {
			throw new UnsupportedOperationException();
		}
	}
}
