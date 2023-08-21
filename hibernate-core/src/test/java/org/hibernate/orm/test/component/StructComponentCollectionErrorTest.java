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

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructAggregate.class)
public class StructComponentCollectionErrorTest {

	@Test
	@JiraKey( "HHH-15830" )
	public void testError() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book.class )
					.getMetadataBuilder()
					.build();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			Assertions.assertTrue( ex.getMessage().contains( "author.tags" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;
		private String title;
		private Person author;
	}

	@Embeddable
	@Struct(name = "person_type")
	public static class Person {
		private String name;
		@ElementCollection
		private List<String> tags;
	}

}
