package org.hibernate.metamodel.source.annotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.hibernate.metamodel.source.Metadata;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Hardy Ferentschik
 */
public class OrmXmlParserTests extends BaseUnitTestCase {
	@Test
	public void testSingleOrmXml() {
		OrmXmlParser parser = new OrmXmlParser( new Metadata( new BasicServiceRegistryImpl( Collections.emptyMap() ) ) );
		Set<String> xmlFiles = new HashSet<String>();
		xmlFiles.add( "org/hibernate/metamodel/source/annotations/orm.xml" );
		parser.parseAndUpdateIndex( xmlFiles, null );
	}

	@Test
	public void testOrmXmlWithOldSchema() {
		OrmXmlParser parser = new OrmXmlParser( new Metadata( new BasicServiceRegistryImpl( Collections.emptyMap() ) ) );
		Set<String> xmlFiles = new HashSet<String>();
		xmlFiles.add( "org/hibernate/metamodel/source/annotations/orm2.xml" );
		parser.parseAndUpdateIndex( xmlFiles, null );
	}
}


