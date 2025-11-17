/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import java.sql.PreparedStatement;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-9287")
@DomainModel(
		annotatedClasses = PooledHiLoSequenceIdentifierTest.SequenceIdentifier.class
)
@SessionFactory
public class PooledHiLoSequenceIdentifierTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSequenceIdentifierGenerator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 5; i++ ) {
						session.persist( new SequenceIdentifier() );
					}
					session.flush();

					assertEquals( 5, countInsertedRows( session ) );

					insertNewRow( session );
					insertNewRow( session );

					assertEquals( 7, countInsertedRows( session ) );

					List<Number> ids = session.createQuery( "SELECT id FROM sequenceIdentifier" ).list();

					assertEquals( 7, ids.size() );

					for ( int i = 0; i < 3; i++ ) {
						session.persist( new SequenceIdentifier() );
					}
					session.flush();

					assertEquals( 10, countInsertedRows( session ) );
				}
		);
	}

	private int countInsertedRows(Session s) {
		return ( (Number) s.createNativeQuery( "SELECT COUNT(*) FROM sequenceIdentifier" )
				.uniqueResult() ).intValue();
	}

	@Entity(name = "sequenceIdentifier")
	public static class SequenceIdentifier {

		@Id
		@GenericGenerator(name = "sampleGenerator", strategy = "enhanced-sequence",
				parameters = {
						@org.hibernate.annotations.Parameter(name = "optimizer", value = "pooled"),
						@org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
						@org.hibernate.annotations.Parameter(name = "increment_size", value = "2")
				}
		)
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "sampleGenerator")
		private Long id;

		private String name;
	}

	private void insertNewRow(Session session) {
		final SessionImplementor si = (SessionImplementor) session;
		final SessionFactoryImplementor sfi = si.getFactory();

		session.doWork(
				connection -> {
					PreparedStatement statement = null;
					try {
						statement = connection.prepareStatement( "INSERT INTO sequenceIdentifier VALUES (?,?)" );
						statement.setObject(
								1,
								((BeforeExecutionGenerator) sfi.getMappingMetamodel().getEntityDescriptor( SequenceIdentifier.class).getGenerator())
										.generate( si, null, null, EventType.INSERT )
						);
						statement.setString( 2,"name" );
						statement.executeUpdate();
					}
					finally {
						if ( statement != null ) {
							statement.close();
						}
					}
				}
		);
	}
}
