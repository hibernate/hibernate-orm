package org.hibernate.orm.test.idgen;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BaseUnitTest
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@Jira("HHH-17653")
public class GeneratorNotAppliedToIdEmbeddableFieldsShouldThrowAnExceptionTest {
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@AfterEach
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	public void testThatAnAnnotationExceptionIsThrown() {
		Exception exception = assertThrows( AnnotationException.class, () -> {
			serviceRegistry = ServiceRegistryUtil.serviceRegistry();
			metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
					.addAnnotatedClass( TestEntity.class )
					.buildMetadata();
		} );
		assertThat( exception.getMessage() ).contains( "Property 'serialValue'" );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENITY")
	public static class TestEntity {

		@Id
		private String id;

		@Embedded
		private TestEmbeddable testEmbeddable;

	}

	@Embeddable
	public static class TestEmbeddable {

		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer serialValue;
	}

}
