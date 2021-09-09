/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@SessionFactory
public class PluralAttributeTests {
	@Test
	public void testJoinAndSelectElementSets(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.createQuery( "select e, s from EntityOfSets e join e.setOfBasics s" ).list();
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getNumberOfJoins( 0 ) ).isEqualTo( 1 );
	}

	@Test
	public void testJoinAndSelectOneToManySets(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.createQuery( "select e, s from EntityOfSets e join e.setOfOneToMany s" ).list();
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		// should be 2 because the association specifies a collection-table
		assertThat( statementInspector.getNumberOfJoins( 0 ) ).isEqualTo( 2 );
	}
}
