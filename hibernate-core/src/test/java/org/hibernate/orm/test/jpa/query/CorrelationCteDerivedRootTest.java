/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.query;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@JiraKey("HHH-17522")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
public class CorrelationCteDerivedRootTest {

	@Test
	public void tesDerivedRoot(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			s.createSelectionQuery("select corrSub.id " +
                                   "from ( " +
                                   "	select 1 as id " +
                                   "	from EntityOfBasics eSub " +
                                   ") sub " +
                                   "left join lateral ( " +
                                   "	select 1 as id " +
                                   "	from EntityOfBasics eSub " +
                                   "	where eSub.id = sub.id " +
                                   ") corrSub on true", Integer.class ).getResultList();
		});
	}

	@Test
	public void tesCte(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			s.createSelectionQuery("with mycte as ( " +
                                   "	select 1 as id " +
                                   "	from EntityOfBasics eSub " +
                                   ") " +
                                   "select corrSub.id " +
                                   "from mycte sub " +
                                   "left join lateral ( " +
                                   "	select 1 as id " +
                                   "	from EntityOfBasics eSub " +
                                   "	where eSub.id = sub.id " +
                                   ") corrSub on true", Integer.class ).getResultList();
		});
	}

}
