package org.hibernate.metamodel.source.annotations.xml;

import org.junit.Test;

import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.MetadataSources;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Hardy Ferentschik
 */
public class OrmXmlParserTests extends BaseUnitTestCase {
	@Test
	public void testSingleOrmXml() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addResource( "org/hibernate/metamodel/source/annotations/xml/orm.xml" );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		// Todo assertions
	}

	@Test
	public void testOrmXmlWithOldSchema() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addResource( "org/hibernate/metamodel/source/annotations/xml/orm-star.xml" );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		// Todo assertions
	}

	@Test(expected = MappingException.class)
	public void testInvalidOrmXmlThrowsException() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addResource( "org/hibernate/metamodel/source/annotations/xml/orm-invalid.xml" );
		sources.buildMetadata();
	}
}


