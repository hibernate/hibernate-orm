package org.hibernate.test.annotations.uniqueconstraint;

import java.util.Iterator;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.junit.Test;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class UniqueConstraintTest extends BaseCoreFunctionalTestCase {

    @Override
	protected Class[] getAnnotatedClasses() {
        return new Class[]{
                Room.class,
                Building.class,
                House.class,
                UniqueNoNameA.class,
                UniqueNoNameB.class
        };
    }

	@Test
    public void testUniquenessConstraintWithSuperclassProperty() throws Exception {
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        Room livingRoom = new Room();
        livingRoom.setId(1l);
        livingRoom.setName("livingRoom");
        s.persist(livingRoom);
        s.flush();
        House house = new House();
        house.setId(1l);
        house.setCost(100);
        house.setHeight(1000l);
        house.setRoom(livingRoom);
        s.persist(house);
        s.flush();
        House house2 = new House();
        house2.setId(2l);
        house2.setCost(100);
        house2.setHeight(1001l);
        house2.setRoom(livingRoom);
        s.persist(house2);
        try {
            s.flush();
            fail("Database constraint non-existant");
        } catch(JDBCException e) {
            //success
        }
        tx.rollback();
        s.close();
    }
	@Test
	@TestForIssue( jiraKey = "HHH-8026" )
	public void testUnNamedConstraints() {
		Database database = metadata().getDatabase();
		org.hibernate.metamodel.spi.relational.Table tableA = null;
		org.hibernate.metamodel.spi.relational.Table tableB = null;
		for(final Schema schema : database.getSchemas()){
			for(final org.hibernate.metamodel.spi.relational.Table table : schema.getTables()){
				if ( table.getPhysicalName().getText().equals( "UniqueNoNameA" ) ) {
					tableA = table;
				}
				else if ( table.getPhysicalName().getText().equals( "UniqueNoNameB" ) ) {
					tableB = table;
				}

			}
		}
		if ( tableA == null || tableB == null ) {
			fail( "Could not find the expected tables." );
		}

		assertFalse( tableA.getUniqueKeys().iterator().next().getName().equals(
				tableB.getUniqueKeys().iterator().next().getName() ) );
	}
	
	@Entity
	@Table( name = "UniqueNoNameA",
			uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
	public static class UniqueNoNameA {
		@Id
		@GeneratedValue
		public long id;
		
		public String name;
	}
	
	@Entity
	@Table( name = "UniqueNoNameB",
			uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
	public static class UniqueNoNameB {
		@Id
		@GeneratedValue
		public long id;
		
		public String name;
	}
    
}
