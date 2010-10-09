package org.hibernate.test.annotations.beanvalidation;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationGroupsTest extends TestCase {
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		try {
			s.persist( ch );
			s.flush();
		}
		catch ( ConstraintViolationException e ) {
			fail( "invalid object should not be validated" );
		}
		try {
			ch.setRadius( null );
			s.flush();
		}
		catch ( ConstraintViolationException e ) {
			fail( "invalid object should not be validated" );
		}
		try {
			s.delete( ch );
			s.flush();
			fail( "invalid object should not be persisted" );
		}
		catch ( ConstraintViolationException e ) {
			assertEquals( 1, e.getConstraintViolations().size() );
			// TODO - seems this explicit case is necessary with JDK 5 (at least on Mac). With Java 6 there is no problem
			Annotation annotation = ( Annotation ) e.getConstraintViolations()
					.iterator()
					.next()
					.getConstraintDescriptor()
					.getAnnotation();
			assertEquals(
					NotNull.class,
					annotation.annotationType()
			);
		}
		tx.rollback();
		s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty(
				"javax.persistence.validation.group.pre-persist",
				""
		);
		cfg.setProperty(
				"javax.persistence.validation.group.pre-update",
				""
		);
		cfg.setProperty(
				"javax.persistence.validation.group.pre-remove",
				Default.class.getName() + ", " + Strict.class.getName()
		);
		cfg.setProperty( "hibernate.validator.apply_to_ddl", "false" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				CupHolder.class
		};
	}
}