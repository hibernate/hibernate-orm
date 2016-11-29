package org.hibernate.test.bytecode.enhancement.inherited;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * @author Luis Barreiro
 * @author Craig Andrews
 */
public class InheritedTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Employee.class, Contractor.class };
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );
	}

	public void execute() {
		Employee charles = new Employee( "Charles", "Engineer" );
		charles.setOca( 1002 );

		// Check that both types of class attributes are being dirty tracked
		EnhancerTestUtils.checkDirtyTracking( charles, "title", "oca" );
		EnhancerTestUtils.clearDirtyTracking( charles );

		// Let's give charles a promotion, this time using method references
		charles.setOca( 99 );
		charles.setTitle( "Manager" );

		EnhancerTestUtils.checkDirtyTracking( charles, "title", "oca" );

		Contractor bob = new Contractor( "Bob", 100 );
		bob.setOca( 1003 );

		// Check that both types of class attributes are being dirty tracked
		EnhancerTestUtils.checkDirtyTracking( bob, "rate", "oca" );
		EnhancerTestUtils.clearDirtyTracking( bob );

		// Let's give bob a rate increase, this time using method references
		bob.setOca( 88 );
		bob.setRate( 200 );

		EnhancerTestUtils.checkDirtyTracking( bob, "rate", "oca" );
	}

	protected void cleanup() {
	}

	@Entity private static abstract class Person {

		@Id private String name;

		@Version private long oca;

		public Person(String name) {
			this();
			this.name = name;
		}

		protected Person() {}

		protected void setOca(long l) {
			this.oca = l;
		}
	}

	@Entity private static class Employee extends Person {

		private String title;

		public Employee(String name, String title) {
			super(name);
			this.title = title;
		}

		public Employee() {}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@Entity private static class Contractor extends Person {

		private Integer rate;

		public Contractor(String name, Integer rate) {
			super(name);
			this.rate = rate;
		}

		public Contractor() {}

		public void setRate(Integer rate) {
			this.rate = rate;
		}
	}
}
