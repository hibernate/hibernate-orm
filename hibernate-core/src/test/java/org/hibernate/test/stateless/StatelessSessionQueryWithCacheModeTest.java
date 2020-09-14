package org.hibernate.test.stateless;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.StatelessSession;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author James Leece
 * @author Nathan Xu
 */
public class StatelessSessionQueryWithCacheModeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12012" )
	@FailureExpected( jiraKey = "HHH-12012" )
	public void testUnsupportedOperationExceptionNotThrown() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				StatelessSession sls = sessionFactory().openStatelessSession( connection );
				sls.getNamedQuery( "test.findAll" )
						.setCacheMode( CacheMode.IGNORE )
						.scroll( ScrollMode.FORWARD_ONLY );
			} );
		} );
	}

	@Entity(name = "TestEntity")
	@Cacheable( false )
	@Table(name = "test")
	@NamedQuery(name = "test.findAll", query = "SELECT t FROM TestEntity t")
	static class TestEntity {

		@Id
		Long id;

	}
}
