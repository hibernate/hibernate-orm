package org.hibernate.test.insertordering;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class InsertOrderingWithOneToOneAndOneToMany extends BaseCoreFunctionalTestCase{

	@Entity
	@Table(name = "Parent")
	public class Parent
	{
	    @Id
	    @GeneratedValue
	    public int id;

	    @OneToOne(cascade = CascadeType.ALL)
	    public Child child;

	    //Without this collection there is no problem
	    @OneToMany
	    private List<Child> otherChilds = new ArrayList<Child>();
	    
	    // Without list initialization the problem disappears too
	    // private List<Child> otherChilds;
	}

	@Entity
	@Table(name = "Child")
	public class Child
	{
	    @Id
	    @GeneratedValue
	    public int id;
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Child.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);

		configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
		configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());

		// With ORDER_INSERTS == FALSE the problem disappears
		configuration.setProperty(AvailableSettings.ORDER_INSERTS, Boolean.TRUE.toString());
	}

	@Test
	public void testOrderInserts() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		Parent parent1 = new Parent();

		Parent parent2 = new Parent();
		parent2.child = new Child();

		session.persist(parent1);
		session.persist(parent2);

		tx.commit();
		session.close();
	}

}
