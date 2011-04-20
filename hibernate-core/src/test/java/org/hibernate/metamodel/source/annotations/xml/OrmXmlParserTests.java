package org.hibernate.metamodel.source.annotations.xml;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.hibernate.metamodel.source.MetadataSources;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Hardy Ferentschik
 */
public class OrmXmlParserTests extends BaseUnitTestCase {
	@Test
	public void testSingleOrmXml() {
		MetadataImpl metadata = (MetadataImpl) new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() ).buildMetadata();
		OrmXmlParser parser = new OrmXmlParser( metadata );
		Set<String> xmlFiles = new HashSet<String>();
		xmlFiles.add( "org/hibernate/metamodel/source/annotations/xml/orm.xml" );
		parser.parseAndUpdateIndex( xmlFiles, null );
	}

	@Test
	public void testOrmXmlWithOldSchema() {
		MetadataImpl metadata = (MetadataImpl) new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() ).buildMetadata();
		OrmXmlParser parser = new OrmXmlParser( metadata );
		Set<String> xmlFiles = new HashSet<String>();
		xmlFiles.add( "org/hibernate/metamodel/source/annotations/xml/orm-star.xml" );
		parser.parseAndUpdateIndex( xmlFiles, null );
	}
}


