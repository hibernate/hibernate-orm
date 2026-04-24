/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@DomainModel(annotatedClasses = {
		CascadeDeleteMultiLevelTest.Trade.class,
		CascadeDeleteMultiLevelTest.Accumulation.class,
		CascadeDeleteMultiLevelTest.Formula.class,
		CascadeDeleteMultiLevelTest.FormulaTerm.class
})
@SessionFactory
@BytecodeEnhanced
public class CascadeDeleteMultiLevelTest {

	@Test
	@JiraKey( "HHH-20146" )
	public void test(SessionFactoryScope scope) {
		Trade trade = new Trade();

		Accumulation accumulation = new Accumulation();
		trade.addAccumulation( accumulation );

		Formula formula = new Formula();
		accumulation.setFormula( formula );

		FormulaTerm formulaTerm = new FormulaTerm();
		formula.setFormulaTerm( formulaTerm );

		scope.inTransaction( em -> {
			em.persist( trade );
		} );

		scope.inTransaction( em -> {
			Trade savedTrade = em.find( Trade.class, trade.id );
			savedTrade.accumulations.clear();
		} );
	}

	@Entity
	public static class Trade {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@OneToMany( mappedBy = "trade", cascade = CascadeType.ALL, orphanRemoval = true )
		List<Accumulation> accumulations = new ArrayList<>();

		public void addAccumulation(Accumulation accumulation) {
			accumulations.add(accumulation);
			accumulation.trade = this;
		}

	}

	@Entity
	public static class Accumulation {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		Trade trade;

		@OneToOne( fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true )
		Formula formula;

		public void setFormula(Formula formula) {
			this.formula = formula;
		}

	}

	@Entity
	public static class Formula {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		@OneToOne( mappedBy = "formula", cascade = CascadeType.ALL, orphanRemoval = true )
		FormulaTerm formulaTerm;

		public void setFormulaTerm(FormulaTerm formulaTerm) {
			this.formulaTerm = formulaTerm;
			formulaTerm.formula = this;
		}

	}

	@Entity
	public static class FormulaTerm {

		@Id
		Long id;

		@MapsId
		@OneToOne( fetch =  FetchType.LAZY )
		Formula formula;

	}

}
