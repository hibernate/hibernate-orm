package org.hibernate.persister.entity;

import static org.junit.Assert.assertEquals;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.test.annotations.persister.CardWithCustomSQL;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Laabidi RAISSI
 *
 */
public class EntityPersisterTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testSchemaNotReplacedInCustomSQL() throws Exception {
		PersistentClass persistentClass = configuration().getClassMapping( CardWithCustomSQL.class.getName() );
		
		final AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory().getEntityPersister( CardWithCustomSQL.class.getName() );
		String insertQuery = persister.getSQLInsertStrings()[0];
		String updateQuery = persister.getSQLUpdateStrings()[0];
		String deleteQuery = persister.getSQLDeleteStrings()[0];
		assertEquals( "Incorrect custom SQL for insert in  Entity: " + persistentClass.getMappedClass(),
				"INSERT INTO FOO", insertQuery );
		
		assertEquals( "Incorrect custom SQL for delete in  Entity: " + persistentClass.getMappedClass(),
				"DELETE FROM FOO", deleteQuery );
		
		assertEquals( "Incorrect custom SQL for update in  Entity: " + persistentClass.getMappedClass(),
				"UPDATE FOO", updateQuery );
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				CardWithCustomSQL.class
		};
	}
}
