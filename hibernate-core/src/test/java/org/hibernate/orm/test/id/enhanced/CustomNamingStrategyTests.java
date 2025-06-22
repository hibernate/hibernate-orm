/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.enhanced;

import java.util.Map;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.KeyValue;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@org.hibernate.testing.orm.junit.ServiceRegistry(
		settings = @Setting(
				name = AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
				value = "org.hibernate.orm.test.id.enhanced.CustomNamingStrategyTests$Strategy"
		)
)
@DomainModel( annotatedClasses = CustomNamingStrategyTests.TheEntity.class )
public class CustomNamingStrategyTests {

	@Test
	public void testIt(DomainModelScope domainModelScope, ServiceRegistryScope serviceRegistryScope) {
		domainModelScope.withHierarchy( TheEntity.class, (entityDescriptor) -> {
			final JdbcServices jdbcServices = serviceRegistryScope.getRegistry().getService( JdbcServices.class );
			final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();

			KeyValue keyValue = entityDescriptor.getIdentifier();
			Dialect dialect = jdbcEnvironment.getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, entityDescriptor);
			final SequenceStyleGenerator generator = (SequenceStyleGenerator) (generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null);

			final String sequenceName = generator.getDatabaseStructure().getPhysicalName().getObjectName().getText();
			assertThat( sequenceName ).isEqualTo( "ents_ids_seq" );
		} );
	}

	public static class Strategy implements ImplicitDatabaseObjectNamingStrategy {
		@Override
		public QualifiedName determineSequenceName(
				Identifier catalogName,
				Identifier schemaName,
				Map<?,?> configValues,
				ServiceRegistry serviceRegistry) {
			final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

			final String rootTableName = ConfigurationHelper.getString( PersistentIdentifierGenerator.TABLE, configValues );
			final String structureName = String.format( "%s_ids_seq", rootTableName );
			return new QualifiedSequenceName(
					catalogName,
					schemaName,
					jdbcEnvironment.getIdentifierHelper().toIdentifier( structureName )
			);
		}

		@Override
		public QualifiedName determineTableName(
				Identifier catalogName,
				Identifier schemaName,
				Map<?,?> configValues,
				ServiceRegistry serviceRegistry) {
			final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

			final String rootTableName = ConfigurationHelper.getString( PersistentIdentifierGenerator.TABLE, configValues );
			final String structureName = String.format( "%s_ids_tbl", rootTableName );
			return new QualifiedSequenceName(
					catalogName,
					schemaName,
					jdbcEnvironment.getIdentifierHelper().toIdentifier( structureName )
			);
		}
	}

	@Entity( name = "TheEntity" )
	@Table( name = "ents" )
	public static class TheEntity {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE )
		private Integer id;
		@Basic
		private String name;

		private TheEntity() {
			// for use by Hibernate
		}

		public TheEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
