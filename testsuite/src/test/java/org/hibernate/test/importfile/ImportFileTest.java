package org.hibernate.test.importfile;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class ImportFileTest extends FunctionalTestCase {

	public void testImportFile() throws Exception {
		Session s = openSession(  );
		final Transaction tx = s.beginTransaction();
		final List<?> list = s.createQuery( "from " + Human.class.getName() ).list();
		assertEquals( "database.sql not imported", 3, list.size() );
		for (Object entity : list) {
			s.delete( entity );
		}
		tx.commit();
		s.close();
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.HBM2DDL_IMPORT_FILE, "/database.sql");
	}

	public ImportFileTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] {
				"importfile/Human.hbm.xml"
		};
	}
}
