package org.hibernate.orm.test.component;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructAggregate.class)
public class StructComponentCollectionError2Test {

	@Test
	@JiraKey( "HHH-15830" )
	public void testError1() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" ).build();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book1.class )
					.getMetadataBuilder()
					.build()
					.buildSessionFactory();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			assertThat( ex.getMessage(), containsString( "participants" ) );
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
		@Column(columnDefinition = "participants_type")
		private List<Person> participants;
	}

	@Test
	@JiraKey( "HHH-15830" )
	public void testError2() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" ).build();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book2.class )
					.getMetadataBuilder()
					.build()
					.buildSessionFactory();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			assertThat( ex.getMessage(), containsString( "participants" ) );
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
		@Column(columnDefinition = "participants_type")
		private Person[] participants;
	}

	@Embeddable
	@Struct(name = "person_type")
	public static class Person {
		private String name;
	}

	@Test
	@JiraKey( "HHH-15830" )
	public void testError3() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book3.class )
					.getMetadataBuilder()
					.build();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			assertThat( ex.getMessage(), containsString( "author.tags" ) );
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
		private Person2 author;
	}

	@Embeddable
	@Struct(name = "person_type")
	public static class Person2 {
		private String name;
		private List<Tag> tags;
	}

	@Test
	@JiraKey( "HHH-15830" )
	public void testError4() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book4.class )
					.getMetadataBuilder()
					.build();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			assertThat( ex.getMessage(), containsString( "author.tags" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "Book")
	public static class Book4 {

		@Id
		@GeneratedValue
		private Long id;
		private String title;
		private Person3 author;
	}

	@Embeddable
	@Struct(name = "person_type")
	public static class Person3 {
		private String name;
		private Tag[] tags;
	}

	@Embeddable
	@Struct(name = "tag_type")
	public static class Tag {
		private String tag;
	}

}
