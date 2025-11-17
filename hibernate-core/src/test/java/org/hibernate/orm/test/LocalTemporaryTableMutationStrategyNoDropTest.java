/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableExporter;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				LocalTemporaryTableMutationStrategyNoDropTest.TestEntity.class
		}
)
@SessionFactory
@ServiceRegistry(
		services = @ServiceRegistry.Service(
				role = ParameterMarkerStrategy.class,
				impl = LocalTemporaryTableMutationStrategyNoDropTest.ParameterMarkerStrategyImpl.class
		)
)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@JiraKey("HHH-16486")
public class LocalTemporaryTableMutationStrategyNoDropTest {

	private static final String MARKER = "$n";

	@Test
	public void testGetSqlTruncateCommand(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final TemporaryTableExporter temporaryTableExporter =
							scope.getSessionFactory().getJdbcServices().getDialect().getTemporaryTableExporter();
					final TemporaryTable idTable = createTemporaryTable( scope );
					final String sqlTruncateCommand = temporaryTableExporter.getSqlTruncateCommand(
							idTable,
							null,
							session
					);
					assertThat( sqlTruncateCommand )
							.contains( idTable.getSessionUidColumn().getColumnName() + " = " + MARKER );
				}
		);

	}

	private static TemporaryTable createTemporaryTable(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		return TemporaryTable.createIdTable(
				scope.getMetadataImplementor().getEntityBinding( TestEntity.class.getName() ),
				basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
				TemporaryTableKind.PERSISTENT,
				jdbcServices.getDialect(),
				new ModelCreationContext( sessionFactory, scope, jdbcServices )
		);
	}

	public static class ParameterMarkerStrategyImpl implements ParameterMarkerStrategy {
		@Override
		public String createMarker(int position, JdbcType jdbcType) {
			return MARKER;
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
		private final JdbcServices jdbcServices;

		public ModelCreationContext(SessionFactoryImplementor sessionFactory, SessionFactoryScope scope, JdbcServices jdbcServices) {
			this.sessionFactory = sessionFactory;
			this.scope = scope;
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
		public TypeConfiguration getTypeConfiguration() {
			return sessionFactory.getTypeConfiguration();
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
			return jdbcServices.getDialect();
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

		@Override
		public Generator getOrCreateIdGenerator(String rootName, PersistentClass persistentClass) {
			return null;
		}
	}
}
