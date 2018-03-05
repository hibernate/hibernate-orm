/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12076;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-12076")
public class HbmMappingJoinClassTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] {
				"Claim.hbm.xml",
				"EwtAssessmentExtension.hbm.xml",
				"Extension.hbm.xml",
				"GapAssessmentExtension.hbm.xml",
				"Settlement.hbm.xml",
				"SettlementExtension.hbm.xml",
				"SettlementTask.hbm.xml",
				"Task.hbm.xml",
				"TaskStatus.hbm.xml",
		};
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/query/hhh12076/";
	}

	@Override
	protected void prepareTest() {
		doInHibernate( this::sessionFactory, session -> {
			TaskStatus taskStatus = new TaskStatus();
			taskStatus.setName("Enabled");
			taskStatus.setDisplayName("Enabled");
			session.save(taskStatus);

			for (long i = 0; i < 10; i++) {
				SettlementTask settlementTask = new SettlementTask();
				settlementTask.setId(i);
				Settlement settlement = new Settlement();
				settlementTask.setLinked(settlement);
				settlementTask.setStatus(taskStatus);

				Claim claim = new Claim();
				claim.setId(i);
				settlement.setClaim(claim);

				for (int j = 0; j < 2; j++) {
					GapAssessmentExtension gapAssessmentExtension = new GapAssessmentExtension();
					gapAssessmentExtension.setSettlement(settlement);
					EwtAssessmentExtension ewtAssessmentExtension = new EwtAssessmentExtension();
					ewtAssessmentExtension.setSettlement(settlement);

					settlement.getExtensions().add(gapAssessmentExtension);
					settlement.getExtensions().add(ewtAssessmentExtension);
				}
				session.save(claim);
				session.save(settlement);
				session.save(settlementTask);
			}
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12076")
	public void testClassExpressionInOnClause() {
		doInHibernate( this::sessionFactory, session -> {
			List<SettlementTask> results = session.createQuery(
				"select " +
				"	rootAlias.id, " +
				"	linked.id, " +
				"	extensions.id " +
				"from SettlementTask as rootAlias " +
				"join rootAlias.linked as linked " +
				"left join linked.extensions as extensions " +
				"	on extensions.class = org.hibernate.query.hhh12076.EwtAssessmentExtension " +
				"where linked.id = :claimId")
			.setParameter("claimId", 1L)
			.getResultList();

			assertNotNull(results);
		} );
	}
}
