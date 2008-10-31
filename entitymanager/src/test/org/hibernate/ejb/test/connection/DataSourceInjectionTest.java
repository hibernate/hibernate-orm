//$Id$
package org.hibernate.ejb.test.connection;

import java.io.File;
import javax.persistence.EntityManagerFactory;

import junit.framework.TestCase;
import org.hibernate.ejb.HibernatePersistence;

/**
 * @author Emmanuel Bernard
 */
public class DataSourceInjectionTest extends TestCase {
	public void testDatasourceInjection() throws Exception {
		File current = new File(".");
		File sub = new File(current, "puroot");
		sub.mkdir();
		PersistenceUnitInfoImpl info = new PersistenceUnitInfoImpl( sub.toURI().toURL(), new String[]{} );
		try {
			EntityManagerFactory emf = ( new HibernatePersistence() ).createContainerEntityManagerFactory( info, null );
			fail( "FakeDatasource should have been used" );
		}
		catch (FakeDataSourceException fde) {
			//success
		}
		finally {
			sub.delete();
		}
	}
}
