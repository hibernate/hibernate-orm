/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {
		ExplicitIdentifierTest.PersonExplicit.class,
		ExplicitIdentifierTest.PersonImplicit.class
})
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.PHYSICAL_NAMING_STRATEGY,
						value = "org.hibernate.orm.test.associations.ExplicitIdentifierTest$CustomPhysicalNamingStrategy")
		}
)
@JiraKey("HHH-14584")
public class ExplicitIdentifierTest {

	@Test
	void testImplicitIdentifier(SessionFactoryScope scope) {

		// implicit named objects have a prefix

		scope.inTransaction( session -> {
			PersonImplicit personImplicit = new PersonImplicit();
			personImplicit.name = "implicit";
			session.persist( personImplicit );
		} );

		scope.inTransaction( session -> {

			assertEquals( "implicit", session.createNativeQuery(
							session.getDialect() instanceof HSQLDialect ?
									"select COLUMN_name from \"TABLE_`ExplicitIdentifierTest$PersonImplicit`\""
									: "select COLUMN_name from TABLE_ExplicitIdentifierTest$PersonImplicit",
							String.class )
					.getSingleResult() );

			if ( session.getDialect().getSequenceSupport().supportsSequences() ) {
				assertEquals( 51, session.createNativeQuery(
						getSqlStringImplicit( session.getDialect() ),
						Integer.class ).getSingleResult() );
			}
			else {
				assertEquals( 51, session.createNativeQuery(
						"select next_val from TABLE_TABLE_ExplicitIdentifierTest$PersonImplicit_SEQ",
						Integer.class ).getSingleResult() );
			}
		} );
	}

	@Test
	void testExplicitIdentifier(SessionFactoryScope scope) {

		// explicit named object don't have a prefix

		scope.inTransaction( session -> {
			PersonExplicit personExplicit = new PersonExplicit();
			personExplicit.name = "explicit";
			session.persist( personExplicit );
		} );

		scope.inTransaction( session -> {

			assertEquals( "explicit", session.createNativeQuery(
							"select name from PersonExplicit",
							String.class )
					.getSingleResult() );

			if ( session.getDialect().getSequenceSupport().supportsSequences() ) {
				assertEquals( 51, session.createNativeQuery(
						getSqlStringExplicit( session.getDialect() ),
						Integer.class ).getSingleResult() );
			}
			else {
				assertEquals( 51, session.createNativeQuery(
						"select next_val from person_explicit_seq",
						Integer.class ).getSingleResult() );
			}
		} );
	}

	private String getSqlStringImplicit(Dialect dialect) {
		if ( dialect instanceof MariaDBDialect || dialect instanceof SQLServerDialect ) {
			return "select next value for SEQUENCE_TABLE_ExplicitIdentifierTest$PersonImplicit_SEQ";
		}
		else if ( dialect instanceof HSQLDialect ) {
			return "call next value for \"SEQUENCE_`TABLE_`ExplicitIdentifierTest$PersonImplicit`_SEQ`\"";
		}
		else if ( dialect instanceof DB2Dialect ) {
			return "values next value for SEQUENCE_TABLE_ExplicitIdentifierTest$PersonImplicit_SEQ";
		}
		else if ( dialect instanceof OracleDialect ) {
			return "select SEQUENCE_TABLE_ExplicitIdentifierTest$PersonImplicit_SEQ.nextval";
		}
		return "select nextval('SEQUENCE_TABLE_ExplicitIdentifierTest$PersonImplicit_SEQ')";
	}

	private String getSqlStringExplicit(Dialect session) {
		if ( session instanceof MariaDBDialect || session instanceof SQLServerDialect ) {
			return "select next value for person_explicit_seq";
		}
		else if ( session instanceof HSQLDialect ) {
			return "call next value for person_explicit_seq";
		}
		else if ( session instanceof DB2Dialect ) {
			return "values next value for person_explicit_seq";
		}
		else if ( session instanceof OracleDialect ) {
			return "select person_explicit_seq.nextval";
		}
		return "select nextval('person_explicit_seq')";
	}

	@AfterAll
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity
	public static class PersonImplicit {

		@Id
		@GeneratedValue
		long id;

		@Column
		String name;
	}

	@Entity
	@Table(name = "PersonExplicit")
	public static class PersonExplicit {

		@Id
		@GeneratedValue(
				strategy = SEQUENCE,
				generator = "person_sequence"
		)
		@SequenceGenerator(name = "person_sequence", sequenceName = "person_explicit_seq")
		long id;

		@Column(name = "name")
		String name;
	}

	public static class CustomPhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {

		@Override
		public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName.isExplicit() ? logicalName
					: jdbcEnvironment.getIdentifierHelper().toIdentifier( "TABLE_" + logicalName,
							logicalName.isQuoted() );
		}

		@Override
		public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName.isExplicit() ? logicalName
					: jdbcEnvironment.getIdentifierHelper().toIdentifier( "SEQUENCE_" + logicalName,
							logicalName.isQuoted() );
		}

		@Override
		public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName.isExplicit() ? logicalName
					: jdbcEnvironment.getIdentifierHelper().toIdentifier( "COLUMN_" + logicalName,
							logicalName.isQuoted() );
		}
	}
}
