/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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
			s.createSelectionQuery( """
				select corrSub.id
				from (
					select 1 as id
					from EntityOfBasics eSub
				) sub
				left join lateral (
					select 1 as id
					from EntityOfBasics eSub
					where eSub.id = sub.id
				) corrSub on true
				""", Integer.class ).getResultList();
		});
	}

	@Test
	public void tesCte(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			s.createSelectionQuery( """
				with mycte as (
					select 1 as id
					from EntityOfBasics eSub
				)
				select corrSub.id
				from mycte sub
				left join lateral (
					select 1 as id
					from EntityOfBasics eSub
					where eSub.id = sub.id
				) corrSub on true
				""", Integer.class ).getResultList();
		});
	}

}
