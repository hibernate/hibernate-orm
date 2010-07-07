package org.hibernate.test.importfile;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit.functional.FunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class ImportFileTest extends FunctionalTestCase {

	public void testImportFile() throws Exception {
		Session s = openSession(  );
		final Transaction tx = s.beginTransaction();
		final List<?> humans = s.createQuery( "from " + Human.class.getName() ).list();
		assertEquals( "humans.sql not imported", 3, humans.size() );

		final List<?> dogs = s.createQuery( "from " + Dog.class.getName() ).list();
		assertEquals( "dogs.sql not imported", 3, dogs.size() );
		for (Object entity : dogs) {
			s.delete( entity );
		}
		for (Object entity : humans) {
			s.delete( entity );
		}
		tx.commit();
		s.close();
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.HBM2DDL_IMPORT_FILES, "/humans.sql,/dogs.sql");
	}

	public ImportFileTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] {
				"importfile/Human.hbm.xml",
				"importfile/Dog.hbm.xml"
		};
	}
}
