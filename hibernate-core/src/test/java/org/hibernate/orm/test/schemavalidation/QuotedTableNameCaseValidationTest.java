package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.testing.jdbc.JdbcUtils;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY;

@JiraKey("HHH-21091")
@RequiresDialect(H2Dialect.class)
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("extractorStrategies")
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = QuotedTableNameCaseValidationTest.TestEntity.class)
public class QuotedTableNameCaseValidationTest implements ServiceRegistryProducer {
	static List<JdbcMetadataAccessStrategy> extractorStrategies() {
		return List.of(
				JdbcMetadataAccessStrategy.GROUPED,
				JdbcMetadataAccessStrategy.INDIVIDUALLY
		);
	}

	private final JdbcMetadataAccessStrategy extractorStrategy;

	public QuotedTableNameCaseValidationTest(JdbcMetadataAccessStrategy extractorStrategy) {
		this.extractorStrategy = extractorStrategy;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder
				.applySetting( DIALECT, LowerCaseQuotedIdentifierH2Dialect.class )
				.applySetting( HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, extractorStrategy )
				.build();
	}

	@BeforeEach
	void setUp(ServiceRegistryScope registryScope) {
		// the name under which a database that stores quoted identifiers in lower case keeps the table
		JdbcUtils.withConnection( registryScope.getRegistry(), connection -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "create table \"test_entity\" (id bigint not null, primary key (id))" );
			}
		} );
	}

	@AfterEach
	void tearDown(ServiceRegistryScope registryScope) {
		JdbcUtils.withConnection( registryScope.getRegistry(), connection -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "drop table \"test_entity\"" );
			}
		} );
	}

	@Test
	public void testValidation(DomainModelScope modelScope) {
		new SchemaValidator().validate( modelScope.getDomainModel() );
	}

	public static class LowerCaseQuotedIdentifierH2Dialect extends H2Dialect {
		@Override
		public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
			super.buildIdentifierHelper( request );
			request.builder().setQuotedCaseStrategy( IdentifierCaseStrategy.LOWER );
			return request.builder().build();
		}
	}

	@Entity
	@Table(name = "`Test_Entity`")
	public static class TestEntity {
		@Id
		private Long id;
	}
}
