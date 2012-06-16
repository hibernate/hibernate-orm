/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.test.criteria;

import java.util.Arrays;

import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Hibernate generates the wrong alias on a Criteria query involving a join
 * on a composite identifier. For example, in the test below without the fix
 * to JoinWalker, it generates this SQL:
 *
 * <code>
 * select
 * this_.role_code as role1_0_1_,
 * this_.is_deleted as is2_0_1_,
 * this_.record_version as record3_0_1_,
 * complexjoi3_.code as code1_0_,
 * complexjoi3_.is_deleted as is2_1_0_,
 * complexjoi3_.record_version as record3_1_0_
 * from
 * list_action_roles this_
 * left outer join
 * roles complexjoi3_
 * on this_.role_code=complexjoi3_.code
 * where
 * this_.is_deleted=?
 * and complexjoi1_.is_deleted=?
 * </code>
 *
 * Which results in this error from the SQL server:
 *
 * <code>
 * Unknown column 'complexjoi1_.is_deleted' in 'where clause'
 * </code>
 *
 * Analysis of the problem:
 *
 * The entity persister class with the composite identifier has a fake
 * property for it, called "_identifierMapper" (see HbmBinder.bindCompositeId()
 * and similarly in Annotations). This property name ends up in the path
 * used by JoinWalker.walkEntityTree() when it calls walkComponentTree().
 * However that path is used by CriteriaJoinWalker.generateTableAlias()
 * to look up the correct criteria (and hence the alias) from the
 * translator, a CriteriaQueryTranslator.
 *
 * The alias was created in CriteriaQueryTranslator.createCriteriaSQLAliasMap
 * for a Criteria without this extra _identifierMapper path component.
 * So when CriteriaJoinWalker tries to use the path with _identifierMapper
 * to look up the criteria to find the correct alias to use, in
 * generateTableAlias(), it doesn't find one, so it generates a new alias.
 *
 * That new alias no longer matches the one that will be rendered out in
 * the WHERE clause, so the WHERE clause will refer to table names using
 * aliases that are not used anywhere else in the query, and the SQL server
 * will fail to parse the statement.
 *
 * The solution appears to be to exclude "_identifierMapper" components in
 * the alias path when building it. I don't know what other implications
 * that might have, but it seems like an implementation nastiness that
 * shouldn't be exposed.
 *
 * @author Chris Wilson
 * @link https://hibernate.onjira.com/browse/HHH-4630
 */

public class ComplexJoinAliasTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ListActionRole.class,
				Role.class
		};
	}

	@Test
	public void testCriteriaThroughCompositeId() throws Exception {
		Session session = openSession();

		Criteria listActionRoles = session.createCriteria( ListActionRole.class );
		listActionRoles.add( Restrictions.eq( "isDeleted", false ) );

		Criteria roles = listActionRoles.createCriteria( "role" );
		roles.add( Restrictions.eq( "isDeleted", false ) );

		assertEquals(
				Arrays.asList( new ListActionRole[] { } ),
				listActionRoles.list()
		);

		session.close();
	}
}
