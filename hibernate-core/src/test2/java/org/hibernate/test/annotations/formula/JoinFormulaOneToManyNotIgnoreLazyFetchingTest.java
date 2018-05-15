/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.formula;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@TestForIssue(jiraKey = "HHH-12770")
public class JoinFormulaOneToManyNotIgnoreLazyFetchingTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, AnnotationBinder.class.getName() )
	);

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000491" );


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Stock.class,
				StockCode.class,
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StockCode code = new StockCode();
			code.setId( 1L );
			code.setCopeType( CodeType.TYPE_A );
			code.setRefNumber( "ABC" );
			entityManager.persist( code );

			Stock stock1 = new Stock();
			stock1.setId( 1L );
			stock1.getCodes().add( code );
			entityManager.persist( stock1 );

			Stock stock2 = new Stock();
			stock2.setId( 2L );
			entityManager.persist( stock2 );
		} );
	}

	@Test
	public void testLazyLoading() {

		assertFalse( triggerable.wasTriggered() );

		List<Stock> stocks = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"SELECT s FROM Stock s", Stock.class )
					.getResultList();
		} );
		assertEquals( 2, stocks.size() );

		try {
			assertEquals( "ABC", stocks.get( 0 ).getCodes().get( 0 ).getRefNumber() );

			fail( "Should have thrown LazyInitializationException" );
		}
		catch (LazyInitializationException expected) {

		}
	}

	@Entity(name = "Stock")
	public static class Stock implements Serializable {

		@Id
		@Column(name = "ID")
		private Long id;

		@OneToMany
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "CODE_ID", referencedColumnName = "ID")
		private List<StockCode> codes = new ArrayList<>(  );

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<StockCode> getCodes() {
			return codes;
		}
	}

	@Entity(name = "StockCode")
	public static class StockCode implements Serializable {

		@Id
		@Column(name = "ID")
		private Long id;

		@Id
		@Enumerated(EnumType.STRING)
		@Column(name = "TYPE")
		private CodeType copeType;

		private String refNumber;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public CodeType getCopeType() {
			return copeType;
		}

		public void setCopeType(CodeType copeType) {
			this.copeType = copeType;
		}

		public String getRefNumber() {
			return refNumber;
		}

		public void setRefNumber(String refNumber) {
			this.refNumber = refNumber;
		}
	}

	public enum CodeType {
		TYPE_A, TYPE_B;
	}

}
