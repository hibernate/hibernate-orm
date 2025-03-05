/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.dataTypes;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Locale;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
@SkipForDialect(dialectClass = OracleDialect.class, reason = "HHH-6834")
@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "HHH-6834")
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "jConnect reports the type code 11 for bigdatetime columns, which is an unknown type code..")
@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase reports the type code 93 for date columns")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class, jiraKey = "HHH-6834")
@DomainModel(
		annotatedClasses = { SomeEntity.class, SomeOtherEntity.class }
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.WRAPPER_ARRAY_HANDLING, value = "ALLOW")
)
public class BasicOperationsTest {

	private static final String SOME_ENTITY_TABLE_NAME = "SOMEENTITY";
	private static final String SOME_OTHER_ENTITY_TABLE_NAME = "SOMEOTHERENTITY";


	@Test
	public void testCreateAndDelete(SessionFactoryScope scope) {
		Date now = new Date();
		SomeEntity someEntity = new SomeEntity( now );
		SomeOtherEntity someOtherEntity = new SomeOtherEntity( 1 );

		scope.inTransaction(
				session -> {
					session.doWork( new ValidateSomeEntityColumns( session ) );
					session.doWork( new ValidateRowCount( session, SOME_ENTITY_TABLE_NAME, 0 ) );
					session.doWork( new ValidateRowCount( session, SOME_OTHER_ENTITY_TABLE_NAME, 0 ) );

					session.persist( someEntity );
					session.persist( someOtherEntity );
				}
		);

		scope.inSession(
				session -> {
					session.doWork( new ValidateRowCount( session, SOME_ENTITY_TABLE_NAME, 1 ) );
					session.doWork( new ValidateRowCount( session, SOME_OTHER_ENTITY_TABLE_NAME, 1 ) );

					try {
						session.beginTransaction();

						session.remove( someEntity );
						session.remove( someOtherEntity );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}

					session.doWork( new ValidateRowCount( session, SOME_ENTITY_TABLE_NAME, 0 ) );
					session.doWork( new ValidateRowCount( session, SOME_OTHER_ENTITY_TABLE_NAME, 0 ) );
				}
		);
	}

	// verify all the expected columns are created
	class ValidateSomeEntityColumns implements Work {
		private SessionImplementor s;

		public ValidateSomeEntityColumns(SessionImplementor s) {
			this.s = s;
		}

		public void execute(Connection connection) throws SQLException {
			// id -> java.util.Date (DATE - becase of explicit TemporalType)
			validateColumn( connection, "ID", java.sql.Types.DATE );

			// timeData -> java.sql.Time (TIME)
			validateColumn( connection, "TIMEDATA", java.sql.Types.TIME );

			// tsData -> java.sql.Timestamp (TIMESTAMP)
			validateColumn( connection, "TSDATA", java.sql.Types.TIMESTAMP );
		}

		private void validateColumn(Connection connection, String columnName, int expectedJdbcTypeCode)
				throws SQLException {
			DatabaseMetaData meta = connection.getMetaData();

			// DBs treat the meta information differently, in particular case sensitivity.
			// We need to use the meta information to find out how to treat names
			String tableNamePattern = generateFinalNamePattern( meta, SOME_ENTITY_TABLE_NAME );
			String columnNamePattern = generateFinalNamePattern( meta, columnName );

			ResultSet columnInfo = meta.getColumns( null, null, tableNamePattern, columnNamePattern );
			s.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().register( columnInfo, columnInfo.getStatement() );
			assertTrue( columnInfo.next() );
			int dataType = columnInfo.getInt( "DATA_TYPE" );
			s.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( columnInfo, columnInfo.getStatement() );
			assertEquals(
					JdbcTypeNameMapper.getTypeName( expectedJdbcTypeCode ),
					JdbcTypeNameMapper.getTypeName( dataType ),
					columnName
			);
		}

		private String generateFinalNamePattern(DatabaseMetaData meta, String name) throws SQLException {
			if ( meta.storesLowerCaseIdentifiers() ) {
				return name.toLowerCase( Locale.ROOT );
			}
			else {
				return name;
			}
		}
	}

	// verify we have the right amount of columns
	class ValidateRowCount implements Work {
		private final int expectedRowCount;
		private final String table;

		private SessionImplementor s;

		public ValidateRowCount(SessionImplementor s, String table, int count) {
			this.s = s;
			this.expectedRowCount = count;
			this.table = table;
		}

		public void execute(Connection connection) throws SQLException {
			Statement st = s.getJdbcCoordinator().getStatementPreparer().createStatement();
			s.getJdbcCoordinator().getResultSetReturn().extract( st, "SELECT COUNT(*) FROM " + table );
			ResultSet result = s.getJdbcCoordinator().getResultSetReturn().extract(
					st,
					"SELECT COUNT(*) FROM " + table
			);
			result.next();
			int rowCount = result.getInt( 1 );
			assertEquals( expectedRowCount, rowCount, "Unexpected row count" );
		}
	}
}
