/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-13347")
@Jpa(
		annotatedClasses = {
				DelimitedIdentifiersTest.SequenceGeneratedEntity.class,
				DelimitedIdentifiersTest.TableGeneratedEntity.class
		},
		xmlMappings = "org/hibernate/orm/test/jpa/xml/delimited-identifiers.xml",
		useCollectingStatementInspector = true
)
public class DelimitedIdentifiersTest {
	@Test
	void xmlDefaultQuotesAllIdentifiers(EntityManagerFactoryScope scope) {
		final var sessionFactory = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		final var dialect = sessionFactory.getJdbcServices().getDialect();
		final var sequencePersister = sessionFactory.getMappingMetamodel()
				.getEntityDescriptor( SequenceGeneratedEntity.class );
		final var tablePersister = sessionFactory.getMappingMetamodel()
				.getEntityDescriptor( TableGeneratedEntity.class );

		assertThat( sequencePersister.getTableName() )
				.isEqualTo( dialect.toQuotedIdentifier( "QuotedSequenceEntity" ) );
		assertThat( tablePersister.getTableName() )
				.isEqualTo( dialect.toQuotedIdentifier( "QuotedTableEntity" ) );

		final var sequenceGenerator = (SequenceStyleGenerator) sequencePersister.getGenerator();
		assertThat( sequenceGenerator.getDatabaseStructure().getPhysicalName().getObjectName().isQuoted() )
				.isTrue();
		assertThat( sequenceGenerator.getDatabaseStructure().getPhysicalName().getObjectName().getText() )
				.isEqualTo( "QuotedSequence" );

		final var tableGenerator = (org.hibernate.id.enhanced.TableGenerator) tablePersister.getGenerator();
		assertThat( tableGenerator.getTableName() ).isEqualTo( "`QuotedGeneratorTable`" );
		assertThat( tableGenerator.getSegmentColumnName() )
				.isEqualTo( dialect.toQuotedIdentifier( "segmentKey" ) );
		assertThat( tableGenerator.getValueColumnName() )
				.isEqualTo( dialect.toQuotedIdentifier( "nextValue" ) );

		assertThat( sessionFactory.getSqlStringGenerationContext().toIdentifier( "RuntimeIdentifier" ).isQuoted() )
				.isTrue();
		assertThat( sessionFactory.getJdbcServices().getJdbcEnvironment()
				.getIdentifierHelper().toIdentifier( "RuntimeIdentifier" ).isQuoted() )
				.isTrue();
		assertThat( sessionFactory.getServiceRegistry()
				.requireService( JdbcEnvironment.class )
				.getIdentifierHelper()
				.toIdentifier( "RegistryIdentifier" )
				.isQuoted() )
				.isFalse();

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( entityManager -> {
			entityManager.persist( new SequenceGeneratedEntity( "sequence" ) );
			entityManager.persist( new TableGeneratedEntity( "table" ) );
		} );

		assertThat( statementInspector.getSqlQueries() )
				.anyMatch( sql -> sql.contains( dialect.toQuotedIdentifier( "QuotedSequenceEntity" ) )
						&& sql.contains( dialect.toQuotedIdentifier( "select" ) ) )
				.anyMatch( sql -> sql.contains( dialect.toQuotedIdentifier( "QuotedTableEntity" ) )
						&& sql.contains( dialect.toQuotedIdentifier( "from" ) ) );
	}

	@Entity(name = "SequenceGeneratedEntity")
	@Table(name = "QuotedSequenceEntity")
	@SequenceGenerator(name = "quoted-sequence", sequenceName = "QuotedSequence", allocationSize = 1)
	public static class SequenceGeneratedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quoted-sequence")
		private Long id;

		@Column(name = "select")
		private String value;

		public SequenceGeneratedEntity() {
		}

		public SequenceGeneratedEntity(String value) {
			this.value = value;
		}
	}

	@Entity(name = "TableGeneratedEntity")
	@Table(name = "QuotedTableEntity")
	@TableGenerator(
			name = "quoted-table",
			table = "QuotedGeneratorTable",
			pkColumnName = "segmentKey",
			valueColumnName = "nextValue",
			pkColumnValue = "QuotedTableEntity",
			allocationSize = 1
	)
	public static class TableGeneratedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "quoted-table")
		private Long id;

		@Column(name = "from")
		private String value;

		public TableGeneratedEntity() {
		}

		public TableGeneratedEntity(String value) {
			this.value = value;
		}
	}
}
