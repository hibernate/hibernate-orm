package org.hibernate.test.annotations.uniqueconstraint;

import java.io.Serializable;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author Nikolay Shestakov
 */
@TestForIssue(jiraKey="HHH-4084")
public class HHH4084Test extends BaseCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Item.class,
				Item2.class
		};
	}
	@Test
	public void testUniqueConstraintWithEmptyColumnNames() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		
		Item2 i0 = new Item2();
		i0.id = 1l;
		i0.value = new Random().nextLong();
		Item2 i1 = new Item2();
		i1.id = 2l;
		i1.value = i0.value;
		
		s.save(i0);
		s.save(i1);
		
		try {
			s.flush();
			Assert.fail("Database constraint non-existant");
		} catch (ConstraintViolationException e) {
			//success
		}
		
		t.rollback();
		s.close();
	}
	
	/**
	 * Minimal entity. Fail in second pass (deffect origin)  
	 */
	@Entity
	@Table(name = "tbl_item_hhh4084",
			uniqueConstraints = {@UniqueConstraint(columnNames = "")}
	)
	public class Item implements Serializable {
		@Id
		protected Long id;
	}
	
	/**
	 * Entity for check creating unique constraint with empty column name
	 */
	@Entity
	@Table(name = "tbl_item_hhh4084_2",
			uniqueConstraints = {@UniqueConstraint(columnNames = { "", "value_" })}
	)
	public class Item2 implements Serializable {
		@Id
		protected Long id;
		@Column( name = "value_" )
		protected Long value;
	}
}
