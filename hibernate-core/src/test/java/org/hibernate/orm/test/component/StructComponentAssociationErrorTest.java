package org.hibernate.orm.test.component;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@JiraKey( "HHH-15831" )
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructAggregate.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructuralArrays.class)
public class StructComponentAssociationErrorTest {

	@Test
	public void testOneToOneMappedBy() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book1.class )
					.getMetadataBuilder()
					.build();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			assertThat( ex.getMessage(), containsString( "authors.favoriteBook" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "Book")
	public static class Book1 {

		@Id
		@GeneratedValue
		private Long id;
		private String title;
		private Person1[] authors;
		@OneToOne(fetch = FetchType.LAZY)
		private Book1 favoredBook;
	}

	@Embeddable
	@Struct(name = "person_type")
	public static class Person1 {
		private String name;
		@OneToOne(mappedBy = "favoredBook", fetch = FetchType.LAZY)
		private Book1 favoriteBook;
	}

	@Test
	public void testOneToManyMappedBy() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book2.class )
					.getMetadataBuilder()
					.build();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			assertThat( ex.getMessage(), containsString( "authors.bookCollection" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "Book")
	public static class Book2 {

		@Id
		@GeneratedValue
		private Long id;
		private String title;
		private Person2[] authors;
		@ManyToOne(fetch = FetchType.LAZY)
		private Book2 mainBook;
	}

	@Embeddable
	@Struct(name = "person_type")
	public static class Person2 {
		private String name;
		@OneToMany(mappedBy = "mainBook")
		private List<Book2> bookCollection;
	}

	@Test
	public void testOneToMany() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book3.class )
					.getMetadataBuilder()
					.build();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			assertThat( ex.getMessage(), containsString( "authors.bookCollection" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "Book")
	public static class Book3 {

		@Id
		@GeneratedValue
		private Long id;
		private String title;
		private Person3[] authors;
	}

	@Embeddable
	@Struct(name = "person_type")
	public static class Person3 {
		private String name;
		@OneToMany
		private List<Book3> bookCollection;
	}

}
