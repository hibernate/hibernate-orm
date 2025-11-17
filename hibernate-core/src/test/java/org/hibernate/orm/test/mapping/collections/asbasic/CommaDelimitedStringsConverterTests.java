/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.asbasic;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = CommaDelimitedStringsConverterTests.Person.class )
@SessionFactory
public class CommaDelimitedStringsConverterTests {
	@Test
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( Person.class, (entityDescriptor) -> {
			final Property nickNamesProperty = entityDescriptor.getProperty( "nickNames" );
			assertThat( nickNamesProperty.getValue() ).isInstanceOf( BasicValue.class );

			final BasicValue valueMapping = (BasicValue) nickNamesProperty.getValue();
			final BasicValue.Resolution<?> resolution = valueMapping.resolve();

			assertThat( resolution.getValueConverter() ).isNotNull();

			assertThat( resolution.getDomainJavaType().getJavaTypeClass() ).isEqualTo( List.class );
			assertThat( resolution.getRelationalJavaType().getJavaTypeClass() ).isEqualTo( String.class );
		} );
	}

	@Test
	public void basicUsageTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Person person = new Person( 1, "John Doe" );
			person.nickNames.add( "John Q. Public" );
			person.nickNames.add( "Joe Public" );
			session.persist( person );
		} );

		scope.inTransaction( (session) -> {
			final Person loaded = session.byId( Person.class ).load( 1 );
			assertThat( loaded.nickNames ).hasSize( 2 );
			assertThat( loaded.nickNames ).containsExactly( "John Q. Public", "Joe Public" );

			loaded.nickNames.add( "Another one" );
		} );

		scope.inTransaction( (session) -> {
			final Person reloaded = session.byId( Person.class ).load( 1 );
			assertThat( reloaded.nickNames ).hasSize( 3 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Table( name = "Person" )
//tag::ex-csv-converter-model[]
	@Entity( name = "Person" )
	public static class Person {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Basic
		@Convert( converter = CommaDelimitedStringsConverter.class )
		private List<String> nickNames;

		// ...

//end::ex-csv-converter-model[]

		private Person() {
			// for use by Hibernate
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
			this.nickNames = new ArrayList<>();
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
//tag::ex-csv-converter-model[]
	}
//end::ex-csv-converter-model[]
}
