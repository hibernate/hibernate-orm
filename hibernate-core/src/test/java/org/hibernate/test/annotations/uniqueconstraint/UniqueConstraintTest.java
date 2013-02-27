package org.hibernate.test.annotations.uniqueconstraint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class UniqueConstraintTest extends BaseCoreFunctionalTestCase {
	
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
		Iterator<org.hibernate.mapping.Table> iterator = configuration().getTableMappings();
		org.hibernate.mapping.Table tableA = null;
		org.hibernate.mapping.Table tableB = null;
		while( iterator.hasNext() ) {
			org.hibernate.mapping.Table table = iterator.next();
			if ( table.getName().equals( "UniqueNoNameA" ) ) {
				tableA = table;
			}
			else if ( table.getName().equals( "UniqueNoNameB" ) ) {
				tableB = table;
			}
		}
		
		if ( tableA == null || tableB == null ) {
			fail( "Could not find the expected tables." );
		}
		
		UniqueKey ukA = (UniqueKey) tableA.getUniqueKeyIterator().next();
		UniqueKey ukB = (UniqueKey) tableB.getUniqueKeyIterator().next();
		assertFalse( ukA.getName().equals( ukB.getName() ) );
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
