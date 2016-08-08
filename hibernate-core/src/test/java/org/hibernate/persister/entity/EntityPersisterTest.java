package org.hibernate.persister.entity;

import static org.junit.Assert.assertEquals;

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

		String className = CardWithCustomSQL.class.getName();
		
		final AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory().getEntityPersister( className );
		String insertQuery = persister.getSQLInsertStrings()[0];
		String updateQuery = persister.getSQLUpdateStrings()[0];
		String deleteQuery = persister.getSQLDeleteStrings()[0];
		assertEquals( "Incorrect custom SQL for insert in  Entity: " + className,
				"INSERT INTO FOO", insertQuery );
		
		assertEquals( "Incorrect custom SQL for delete in  Entity: " + className,
				"DELETE FROM FOO", deleteQuery );
		
		assertEquals( "Incorrect custom SQL for update in  Entity: " + className,
				"UPDATE FOO", updateQuery );
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				CardWithCustomSQL.class
		};
	}
}
