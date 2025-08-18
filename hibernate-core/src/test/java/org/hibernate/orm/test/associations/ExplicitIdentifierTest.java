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
	void testExplicitIdentifier(SessionFactoryScope scope) {

		scope.inTransaction( session -> {
			PersonImplicit personImplicit = new PersonImplicit();
			personImplicit.name = "implicit";
			session.persist( personImplicit );

			PersonExplicit personExplicit = new PersonExplicit();
			personExplicit.name = "explicit";
			session.persist( personExplicit );
		} );

		scope.inTransaction( session -> {

			// implicit named objects have a prefix
			assertEquals( "implicit", session.createNativeQuery(
							"select COLUMN_name from TABLE_ExplicitIdentifierTest$PersonImplicit",
							String.class )
					.getSingleResult() );

			if ( session.getDialect().getSequenceSupport().supportsSequences() ) {
				assertEquals( 51, session.createNativeQuery(
						"select nextval('SEQUENCE_TABLE_ExplicitIdentifierTest$PersonImplicit_SEQ')",
						Integer.class ).getSingleResult() );
			}
			else {
				assertEquals( 51, session.createNativeQuery(
						"select next_val from TABLE_TABLE_ExplicitIdentifierTest$PersonImplicit_SEQ",
						Integer.class ).getSingleResult() );
			}

			// explicit named object don't have a prefix
			assertEquals( "explicit", session.createNativeQuery(
							"select name from PersonExplicit",
							String.class )
					.getSingleResult() );

			if ( session.getDialect().getSequenceSupport().supportsSequences() ) {
				assertEquals( 51, session.createNativeQuery(
					"select nextval('person_explicit_seq')",
						Integer.class ).getSingleResult() );
			}
			else {
				assertEquals( 51, session.createNativeQuery(
						"select next_val from person_explicit_seq",
						Integer.class ).getSingleResult() );
			}
		} );
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
