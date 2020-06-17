package org.hibernate.orm.test.annotations.embeddables.collection;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractEmbeddableWithManyToManyTest {
	@Test
	public void test() {
		try {
			BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
			final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder(
					bootstrapServiceRegistryBuilder.build() );
			StandardServiceRegistry ssr = ssrb.build();
			MetadataSources metadataSources = new MetadataSources( ssr );
			addResources( metadataSources );
			addAnnotatedClasses(metadataSources);

			metadataSources.buildMetadata();
			fail( "Should throw AnnotationException!" );
		}
		catch (AnnotationException expected) {
			assertTrue( expected.getMessage().startsWith(
					"@OneToMany, @ManyToMany or @ElementCollection cannot be used inside an @Embeddable that is also contained within an @ElementCollection"
			) );
		}
	}

	protected void addAnnotatedClasses(MetadataSources metadataSources){

	}

	protected void addResources(MetadataSources metadataSources){

	}
}
