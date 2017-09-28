/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.BasicBinder;
import org.hibernate.type.descriptor.sql.spi.BasicExtractor;
import org.hibernate.type.descriptor.sql.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * An SQL dialect for Postgres 8.2 and later, adds support for "if exists" when dropping tables
 * 
 * @author edalquist
 */
public class PostgreSQL82Dialect extends PostgreSQL81Dialect {
	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		// HHH-9562
		typeContributions.contributeSqlTypeDescriptor( PostgresUUIDType.INSTANCE );
	}

	@Override
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy(
				new StandardIdTableSupport(
						new LocalTempTableExporter() {
							@Override
							public String getCreateCommand() {
								return "create temporary  table";
							}
						}
				)
		);
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	private static class PostgresUUIDType implements SqlTypeDescriptor {
		/**
		 * Singleton access
		 */
		public static final PostgresUUIDType INSTANCE = new PostgresUUIDType();

		/**
		 * Postgres reports its UUID type as {@link java.sql.Types#OTHER}.  Unfortunately
		 * it reports a lot of its types as {@link java.sql.Types#OTHER}, making that
		 * value useless for distinguishing one SqlTypeDescriptor from another.
		 * So here we define a "magic value" that is a (hopefully no collisions)
		 * unique key within the {@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry}
		 */
		public static final int JDBC_TYPE_CODE = 3975;

		@Override
		public int getJdbcTypeCode() {
			return JDBC_TYPE_CODE;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		public <J> BasicJavaDescriptor<J> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<J>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( UUID.class );
		}

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
			return null;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					st.setObject( index, javaTypeDescriptor.unwrap( value, UUID.class, options ), Types.OTHER );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setObject( name, javaTypeDescriptor.unwrap( value, UUID.class, options ), Types.OTHER );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int position, WrapperOptions options)
						throws SQLException {
					return javaTypeDescriptor.wrap( rs.getObject( position ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( index ), options );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						String name,
						WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( name ), options );
				}
			};
		}
	}
}
