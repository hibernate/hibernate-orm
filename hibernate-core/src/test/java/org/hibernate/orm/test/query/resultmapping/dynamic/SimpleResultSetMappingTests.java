/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping.dynamic;

import jakarta.persistence.Tuple;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for [ResultSetMapping] creation/usage.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = { Book.class, DropDownItem.class })
@SessionFactory
public class SimpleResultSetMappingTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Book( 1, "The Fellowship of the Ring", "123-456", LocalDate.now() ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testColumnMappings(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.column( "id", Integer.class );
			var result = session.createNativeQuery( "select id from books", mapping ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ) ).isInstanceOf( Integer.class );
		} );
		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.column( "name", String.class );
			var result = session.createNativeQuery( "select name from books", mapping ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ) ).isInstanceOf( String.class );
		} );
		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.column( "published", LocalDate.class );
			var result = session.createNativeQuery( "select published from books", mapping ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ) ).isInstanceOf( LocalDate.class );
		} );
	}

	/// @see NamedToDynamicTests#testConstructorConversions
	@Test
	void testConstructorMappings(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.constructor(
					Book.class,
					ResultSetMapping.column( "id", Integer.class ),
					ResultSetMapping.column( "name", String.class ),
					ResultSetMapping.column( "isbn", String.class ),
					ResultSetMapping.column( "published", LocalDate.class )
			);
			var result = session.createNativeQuery( "select * from books", mapping ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ) ).isInstanceOf( Book.class );
		} );

		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.constructor(
					DropDownItem.class,
					ResultSetMapping.column( "id", Integer.class ),
					ResultSetMapping.column( "name", String.class )
			);
			var result = session.createNativeQuery( "select * from books", mapping ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ) ).isInstanceOf( DropDownItem.class );
		} );

		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.constructor(
					NestedDropDownItem.class,
					ResultSetMapping.column( "isbn", String.class ),
					ResultSetMapping.column( "name", String.class ),
					ResultSetMapping.constructor(
							DropDownItem.class,
							ResultSetMapping.column( "id", Integer.class ),
							ResultSetMapping.column( "name", String.class )
					)
			);
			var result = session.createNativeQuery( "select * from books", mapping ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ) ).isInstanceOf( NestedDropDownItem.class );
		} );
	}

	@Test
	void testEntityMapping(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.entity(
					Book.class,
					ResultSetMapping.field( Book_.id, "id" ),
					ResultSetMapping.field( Book_.name, "name" ),
					ResultSetMapping.field( Book_.isbn, "isbn" ),
					ResultSetMapping.field( Book_.published, "published" )
			);
			var result = session.createNativeQuery( "select * from books", mapping ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ) ).isInstanceOf( Book.class );
		} );
	}

	@Test
	void testTupleMapping(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.tuple(
					ResultSetMapping.column( "id", Integer.class ),
					ResultSetMapping.column( "name", String.class ),
					ResultSetMapping.column( "isbn", String.class ),
					ResultSetMapping.column( "published", LocalDate.class )
			);
			var result = session.createNativeQuery( "select * from books", mapping ).list();
			assertThat( result ).hasSize( 1 );

			final Tuple tuple = result.get( 0 );
			assertThat( tuple.getElements() ).hasSize( 4 );
			// should be the id
			assertThat( tuple.get( "id" ) ).isEqualTo( 1 );
			assertThat( tuple.get( 0 ) ).isEqualTo( 1 );
			// should be the name
			assertThat( tuple.get( "name" ) ).isEqualTo( "The Fellowship of the Ring" );
			assertThat( tuple.get( 1 ) ).isEqualTo( "The Fellowship of the Ring" );
		} );

		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.tuple(
					ResultSetMapping.constructor(
							DropDownItem.class,
							ResultSetMapping.column( "id", Integer.class ),
							ResultSetMapping.column( "name", String.class )
					),
					ResultSetMapping.column( "isbn", String.class ),
					ResultSetMapping.column( "published", LocalDate.class )
			);
			var result = session.createNativeQuery( "select * from books", mapping ).list();
			assertThat( result ).hasSize( 1 );

			final Tuple tuple = result.get( 0 );
			assertThat( tuple.getElements() ).hasSize( 3 );
			// note: currently not an easy way to alias the constructor mapping
			assertThat( tuple.get( 0 ) ).isInstanceOf( DropDownItem.class );
			assertThat( tuple.get( 1 ) ).isEqualTo( "123-456" );
			assertThat( tuple.get( "isbn" ) ).isEqualTo( "123-456" );
		} );
	}

	@Test
	void testCompoundMapping(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var mapping = ResultSetMapping.compound(
					ResultSetMapping.column( "id", Integer.class ),
					ResultSetMapping.column( "name", String.class ),
					ResultSetMapping.column( "isbn", String.class ),
					ResultSetMapping.column( "published", LocalDate.class )
			);
			var result = session.createNativeQuery( "select * from books", mapping ).list();
			assertThat( result ).hasSize( 1 );
			final Object[] row = result.get( 0 );
			assertThat( row ).hasSize( 4 );
			// should be the id
			assertThat( row[0] ).isEqualTo( 1 );
			// should be the name
			assertThat( row[1] ).isEqualTo( "The Fellowship of the Ring" );
		} );
	}
}
