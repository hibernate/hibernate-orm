/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.test.type.array.StringArrayType;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12292")
@RequiresDialect(H2Dialect.class)
public class QueryParametersValidationArrayTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Event.class};
	}

	@Test
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentException() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery(
				"select id " +
				"from Event " +
				"where readings = :readings" )
			.unwrap( NativeQuery.class )
			.setParameter( "readings", new String[]{null, "a"}, StringArrayType.INSTANCE )
			.getResultList();
		});
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(columnDefinition = "ARRAY(1)")
		private String[] readings;
	}

}
