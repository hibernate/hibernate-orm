package org.hibernate.ejb.test.metadata;

import javax.persistence.EntityManagerFactory;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class SecondMetadataTest extends TestCase {

	public void testBaseOfService() throws Exception {
		EntityManagerFactory emf = factory;
		assertNotNull( emf.getMetamodel() );
		assertNotNull( emf.getMetamodel().entity( DeskWithRawType.class ) );
		assertNotNull( emf.getMetamodel().entity( EmployeeWithRawType.class ) );
		assertNotNull( emf.getMetamodel().entity( SimpleMedicalHistory.class ) );
		assertNotNull( emf.getMetamodel().entity( SimplePerson.class ) );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				DeskWithRawType.class,
				EmployeeWithRawType.class,
				SimpleMedicalHistory.class,
				SimplePerson.class
		};
	}
}
