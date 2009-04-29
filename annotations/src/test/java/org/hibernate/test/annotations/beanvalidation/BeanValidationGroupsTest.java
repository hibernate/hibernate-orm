package org.hibernate.test.annotations.beanvalidation;

import java.math.BigDecimal;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationGroupsTest extends TestCase {
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		Session s = openSession(  );
		Transaction tx = s.beginTransaction();
		try {
			s.persist( ch );
			s.flush();
		}
		catch ( ConstraintViolationException e ) {
			fail("invalid object should not be validated");
		}
		try {
			ch.setRadius( null );
			s.flush();
		}
		catch ( ConstraintViolationException e ) {
			fail("invalid object should not be validated");
		}
		try {
			s.delete( ch );
			s.flush();
			fail("invalid object should not be persisted");
		}
		catch ( ConstraintViolationException e ) {
			assertEquals( 1, e.getConstraintViolations().size() );
			assertEquals( NotNull.class,
						e.getConstraintViolations().iterator().next().getConstraintDescriptor().getAnnotation().annotationType()
					);
		}
		tx.rollback();
		s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "javax.persistence.validation.group.pre-persist",
				"" );
		cfg.setProperty( "javax.persistence.validation.group.pre-update",
				"" );
		cfg.setProperty( "javax.persistence.validation.group.pre-remove",
				Default.class.getName() + ", " + Strict.class.getName() );
	}

	protected Class<?>[] getMappings() {
		return new Class<?>[] {
				CupHolder.class
		};
	}
}