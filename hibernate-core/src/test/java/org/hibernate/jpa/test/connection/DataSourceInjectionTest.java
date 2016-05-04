/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.jpa.test.connection;

import java.io.File;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.jpa.HibernatePersistenceProvider;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class DataSourceInjectionTest {
	EntityManagerFactory emf;
    @Test
	public void testDatasourceInjection() throws Exception {
		File current = new File(".");
		File sub = new File(current, "puroot");
		sub.mkdir();
		PersistenceUnitInfoImpl info = new PersistenceUnitInfoImpl( sub.toURI().toURL(), new String[]{} );
		try {
			emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory( info, null );
			try {
				emf.createEntityManager().createQuery( "select i from Item i" ).getResultList();
			}
			finally {
				try {
					emf.close();
				}
				catch (Exception ignore) {
					int i = 0;
				}
			}
			Assert.fail( "FakeDatasource should have been used" );
		}
		catch (PersistenceException pe) {
			if(emf != null){
				emf.close();
			}
			Assert.assertTrue( pe.getCause() instanceof FakeDataSourceException );
		}
		catch (FakeDataSourceException fde) {
			//success
		}
		finally {
			sub.delete();
		}
	}
}
