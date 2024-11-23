/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinedsubclass;

import java.util.Map;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.Test;

import org.hamcrest.core.StringStartsWith;

import static org.hamcrest.CoreMatchers.not;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertThat;

/**
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14153" )
public class SingleTableUpdateQueryTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	public String[] getMappings() {
		return new String[] { "org/hibernate/test/joinedsubclass/Person.hbm.xml" };
	}

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInterceptor = new SQLStatementInterceptor( options );
	}

	@Test
	public void testSingeTableUpdateWithoutOtherTableInvolvement() {
		sqlStatementInterceptor.clear();

		/*
		 * update
		 *         JPerson
		 *     set
		 *         name=(name||?)
		 *     where
		 *         sex=?
		 */

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "UPDATE Person p SET p.name = concat(p.name, :nameSuffix) WHERE p.sex = :sex" )
					.setParameter( "nameSuffix", "(f)" )
					.setParameter( "sex", 'f' )
					.executeUpdate();
		} );

		sqlStatementInterceptor.getSqlQueries().forEach(
				// currently SQLStatementInterceptor is not able to catch id table creation SQL
				sql -> assertThat( sql, not( StringStartsWith.startsWith( "insert into " ) ) )
		);
	}

	@Test
	public void testSingeTableUpdateWithOtherTableInvolvement() {
		sqlStatementInterceptor.clear();

		/*
		 * update
		 *         JPerson
		 *     set
		 *         name=name
		 *     where
		 *         (
		 *             person_id
		 *         ) IN (
		 *             select
		 *                 customer0_.person_id as person_id
		 *             from
		 *                 JManager customer0_
		 *             inner join
		 *                 JPerson customer0_1_
		 *                     on customer0_.person_id=customer0_1_.person_id
		 *             )
		 */

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "UPDATE Customer SET name = name" ) // 'name' is from 'Person' parent entity
					.executeUpdate();
		} );

		sqlStatementInterceptor.getSqlQueries().forEach(
				// currently SQLStatementInterceptor is not able to catch id table creation SQL
				sql -> assertThat( sql, not( StringStartsWith.startsWith( "insert into " ) ) )
		);
	}

}