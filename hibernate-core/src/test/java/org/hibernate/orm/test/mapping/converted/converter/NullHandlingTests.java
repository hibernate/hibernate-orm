/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {NullHandlingTests.TheEntity.class})
@SessionFactory
public class NullHandlingTests {

	@Test
	@JiraKey( value = "HHH-8697" )
	public void testNullReplacementOnBinding(SessionFactoryScope scope) {
		TheEntity theEntity = new TheEntity( 1 );

		// at this point TheEntity.sex is null
		// lets make sure that the converter is given a chance to adjust that to UNKNOWN...
		scope.inTransaction( session -> session.persist( theEntity ) );

		scope.inTransaction(  session -> {
					session.doWork(
							conn -> {
								try (Statement st = conn.createStatement()) {
									st.execute( "select sex from the_entity where id = 1" );
									ResultSet rs = st.getResultSet();
									if ( !rs.next() ) {
										throw new RuntimeException( "Could not locate inserted row" );
									}

									String sexDbValue = rs.getString( 1 );

									if ( rs.next() ) {
										throw new RuntimeException( "Found more than one row" );
									}

									assertEquals( Sex.UNKNOWN.name().toLowerCase( Locale.ENGLISH ), sexDbValue );
								}
							}
					);
				} );

		scope.inTransaction( session -> session.remove( theEntity ) );
	}

	@Test
	@JiraKey( value = "HHH-9320" )
	public void testNullReplacementOnExtraction(SessionFactoryScope scope) {
		scope.inTransaction(  session -> {
			session.doWork(
					connection -> connection.createStatement().execute( "insert into the_entity(id, sex) values (1, null)" )
			);
		} );

		// at this point TheEntity.sex is null in the database
		// lets load it and make sure that the converter is given a chance to adjust that to UNKNOWN...
		TheEntity theEntity = scope.fromTransaction( session -> session.find( TheEntity.class, 1 ) );
		assertEquals( Sex.UNKNOWN, theEntity.sex );

		scope.inTransaction( session -> session.remove( theEntity ) );
	}

	@Entity( name = "TheEntity" )
	@Table( name = "the_entity" )
	public static class TheEntity {
		@Id
		public Integer id;

		@Convert( converter = SexConverter.class )
		public Sex sex;

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;
		}
	}

	public static enum Sex {
		MALE,
		FEMALE,
		UNKNOWN
	}

	public static class SexConverter implements AttributeConverter<Sex,String> {
		@Override
		public String convertToDatabaseColumn(Sex attribute) {
			// HHH-8697
			if ( attribute == null ) {
				return Sex.UNKNOWN.name().toLowerCase( Locale.ENGLISH );
			}

			return attribute.name().toLowerCase( Locale.ENGLISH );
		}

		@Override
		public Sex convertToEntityAttribute(String dbData) {
			// HHH-9320
			if ( dbData == null ) {
				return Sex.UNKNOWN;
			}

			return Sex.valueOf( dbData.toUpperCase( Locale.ENGLISH ) );
		}
	}
}
