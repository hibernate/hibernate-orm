//$Id: A320.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
package org.hibernate.test.annotations.onetoone.primarykey;

import junit.framework.TestCase;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.SQLServerDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test harness for ANN-742.
 * 
 * @author Hardy Ferentschik
 * 
 */
public class NullablePrimaryKeyTest extends TestCase {

	private Logger log = LoggerFactory.getLogger(NullablePrimaryKeyTest.class);

	public void testGeneratedSql() {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(Address.class);
			config.addAnnotatedClass(Person.class);
			config.buildSessionFactory();
			String[] schema = config
					.generateSchemaCreationScript(new SQLServerDialect());
			for (String s : schema) {
				log.debug(s);
			}
			String expectedMappingTableSql = "create table personAddress (address_id numeric(19,0) null, " +
					"person_id numeric(19,0) not null, primary key (person_id))";
			assertEquals("Wrong SQL", expectedMappingTableSql, schema[2]);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
