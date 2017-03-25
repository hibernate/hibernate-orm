package org.hibernate.test.bytecode.enhancement.mapped;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.Version;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * @author Luis Barreiro
 */
public class MappedSuperclassTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Employee.class };
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
	}

	protected void cleanup() {
	}

	@MappedSuperclass private static class Person {

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

	@Table(name="MSTT_EMPLOYEE")
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
}
