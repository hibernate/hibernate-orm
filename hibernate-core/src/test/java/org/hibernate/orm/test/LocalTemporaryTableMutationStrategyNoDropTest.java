/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test;

import java.util.Map;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temptable.StandardTemporaryTableExporter;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				LocalTemporaryTableMutationStrategyNoDropTest.TestEntity.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, value = "false")
		},
		services = @ServiceRegistry.Service(
				role = ParameterMarkerStrategy.class,
				impl = LocalTemporaryTableMutationStrategyNoDropTest.ParameterMarkerStrategyImpl.class
		)
)
@JiraKey("HHH-16486")
public class LocalTemporaryTableMutationStrategyNoDropTest {

	private static final String MARKER = "$n";

	@Test
	public void testGetSqlTruncateCommand(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					StandardTemporaryTableExporter standardTemporaryTableExporter
							= createStandardTemporaryTableExporter( session );
					TemporaryTable idTable = createTemporaryTable( scope, session );
					String sqlTruncateCommand = standardTemporaryTableExporter.getSqlTruncateCommand(
							idTable,
							null,
							session
					);
					assertThat( sqlTruncateCommand )
							.contains( idTable.getSessionUidColumn().getColumnName() + " = " + MARKER );
				}
		);

	}

	private static StandardTemporaryTableExporter createStandardTemporaryTableExporter(SessionImplementor session) {
		return new StandardTemporaryTableExporter( getDialect() );
	}

	private static Dialect getDialect() {
		return new TestDialect();
	}

	private static TemporaryTable createTemporaryTable(
			SessionFactoryScope scope,
			SessionImplementor session) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		Dialect dialect = getDialect();
		return TemporaryTable.createIdTable(
				session.getEntityPersister( null, new TestEntity() ),
				basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
				dialect,
				new ModelCreationContext( sessionFactory, scope, dialect, jdbcServices )
		);
	}

	public static class ParameterMarkerStrategyImpl implements ParameterMarkerStrategy {
		@Override
		public String createMarker(int position, JdbcType jdbcType) {
			return MARKER;
		}
	}

	public static class TestDialect extends Dialect {

		@Override
		public boolean supportsTemporaryTables() {
			return true;
		}

		@Override
		public TemporaryTableKind getSupportedTemporaryTableKind() {
			return TemporaryTableKind.PERSISTENT;
		}
	}


	@Entity(name = "ParentEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class ParentEntity {
		@Id
		@GeneratedValue
		public Long id;
		String name;
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends ParentEntity {


	}

	private static class ModelCreationContext implements RuntimeModelCreationContext, GeneratorSettings {

		private final SessionFactoryImplementor sessionFactory;
		private final SessionFactoryScope scope;
		private final Dialect dialect;
		private final JdbcServices jdbcServices;

		public ModelCreationContext(SessionFactoryImplementor sessionFactory, SessionFactoryScope scope, Dialect dialect, JdbcServices jdbcServices) {
			this.sessionFactory = sessionFactory;
			this.scope = scope;
			this.dialect = dialect;
			this.jdbcServices = jdbcServices;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return sessionFactory;
		}

		@Override
		public BootstrapContext getBootstrapContext() {
			return null;
		}

		@Override
		public MetadataImplementor getBootModel() {
			return scope.getMetadataImplementor();
		}

		@Override
		public MappingMetamodelImplementor getDomainModel() {
			return null;
		}

		@Override
		public SqmFunctionRegistry getFunctionRegistry() {
			return null;
		}

		@Override
		public Map<String, Object> getSettings() {
			return sessionFactory.getProperties();
		}

		@Override
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		public CacheImplementor getCache() {
			return null;
		}

		@Override
		public SessionFactoryOptions getSessionFactoryOptions() {
			return sessionFactory.getSessionFactoryOptions();
		}

		@Override
		public JdbcServices getJdbcServices() {
			return jdbcServices;
		}

		@Override
		public SqlStringGenerationContext getSqlStringGenerationContext() {
			return SqlStringGenerationContextImpl.fromExplicit(
					jdbcServices.getJdbcEnvironment(),
					scope.getMetadataImplementor().getDatabase(),
					null,
					null
			);
		}

		@Override
		public org.hibernate.service.ServiceRegistry getServiceRegistry() {
			return sessionFactory.getServiceRegistry();
		}

		@Override
		public Map<String, Generator> getGenerators() {
			return emptyMap();
		}

		@Override
		public String getDefaultCatalog() {
			return null;
		}

		@Override
		public String getDefaultSchema() {
			return null;
		}

		@Override
		public GeneratorSettings getGeneratorSettings() {
			return this;
		}
	}
}
