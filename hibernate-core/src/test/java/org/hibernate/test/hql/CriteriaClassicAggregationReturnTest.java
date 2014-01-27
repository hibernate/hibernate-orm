/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.test.hql;
import java.util.Collections;

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.function.ClassicAvgFunction;
import org.hibernate.dialect.function.ClassicCountFunction;
import org.hibernate.dialect.function.ClassicSumFunction;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.internal.classic.ClassicQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel
public class CriteriaClassicAggregationReturnTest extends QueryTranslatorTestCase {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.addSqlFunction( "count", new ClassicCountFunction() );
		cfg.addSqlFunction( "avg", new ClassicAvgFunction() );
		cfg.addSqlFunction( "sum", new ClassicSumFunction() );
	}

	@Test
	public void testClassicHQLAggregationReturnTypes() {
		// EJB3: COUNT returns Long
		QueryTranslatorImpl translator = createNewQueryTranslator( "select count(*) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", IntegerType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select count(h.heightInches) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", IntegerType.INSTANCE, translator.getReturnTypes()[0] );

		// MAX, MIN return the type of the state-field to which they are applied.
		translator = createNewQueryTranslator( "select max(h.heightInches) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", DoubleType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select max(h.id) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", LongType.INSTANCE, translator.getReturnTypes()[0] );

		// AVG returns Float integrals, and otherwise the field type.
		translator = createNewQueryTranslator( "select avg(h.heightInches) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", DoubleType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select avg(h.id) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", FloatType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select avg(h.bigIntegerValue) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", BigIntegerType.INSTANCE, translator.getReturnTypes()[0] );

        // SUM returns underlying type sum
 	    translator = createNewQueryTranslator( "select sum(h.id) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", LongType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.intValue) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", IntegerType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.heightInches) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", DoubleType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.floatValue) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", FloatType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.bigIntegerValue) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", BigIntegerType.INSTANCE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.bigDecimalValue) from Human h", sessionFactory() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", BigDecimalType.INSTANCE, translator.getReturnTypes()[0] );

		// special case to test classicquery special case handling of count(*)
		String hql = "select count(*) from Human h";
		QueryTranslatorFactory classic = new ClassicQueryTranslatorFactory();
		QueryTranslator oldQueryTranslator = classic.createQueryTranslator( hql, hql, Collections.EMPTY_MAP,
				sessionFactory(), null );
		oldQueryTranslator.compile( Collections.EMPTY_MAP, true);
		assertEquals( "incorrect return type count", 1, oldQueryTranslator.getReturnTypes().length );
		assertEquals( "incorrect return type", IntegerType.INSTANCE, oldQueryTranslator.getReturnTypes()[0] );

	}
}
