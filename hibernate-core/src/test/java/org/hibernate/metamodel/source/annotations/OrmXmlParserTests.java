package org.hibernate.metamodel.source.annotations;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Hardy Ferentschik
 */
public class OrmXmlParserTests extends BaseUnitTestCase {
	@Test
	public void testSingleOrmXml() {
		OrmXmlParser parser = new OrmXmlParser();
		Set<String> xmlFiles = new HashSet<String>();
		xmlFiles.add( "org/hibernate/metamodel/source/annotations/orm.xml" );
		parser.parseAndUpdateIndex( xmlFiles, null );

	}
}


