package org.hibernate.metamodel.binding;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.Where;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */
public class MiscAnnotationBindingTest extends BaseUnitTestCase {
	@Test
	public void testWhereFilter() {


		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addAnnotatedClass( Foo.class );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();

		EntityBinding binding = metadata.getEntityBinding( MiscAnnotationBindingTest.class.getSimpleName() + "$" + Foo.class.getSimpleName() );
		assertEquals( "Wrong where filter", "1=1", binding.getWhereFilter() );
	}

	@Entity
	@Where(clause = "1=1")
	class Foo {
		@Id
		private long id;
	}
}


