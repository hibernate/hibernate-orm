/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.inheritance.discriminator;

import java.util.Collections;

import org.hibernate.Session;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.hql.spi.QueryTranslator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.inheritance.discriminator.multidowncast.IntIdEntity;
import org.hibernate.test.inheritance.discriminator.multidowncast.NameObject;
import org.hibernate.test.inheritance.discriminator.multidowncast.PolymorphicBase;
import org.hibernate.test.inheritance.discriminator.multidowncast.PolymorphicPropertyBase;
import org.hibernate.test.inheritance.discriminator.multidowncast.PolymorphicPropertyMapBase;
import org.hibernate.test.inheritance.discriminator.multidowncast.PolymorphicPropertySub1;
import org.hibernate.test.inheritance.discriminator.multidowncast.PolymorphicPropertySub2;
import org.hibernate.test.inheritance.discriminator.multidowncast.PolymorphicSub1;
import org.hibernate.test.inheritance.discriminator.multidowncast.PolymorphicSub2;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Christian Beikov
 */
public class MultiInheritanceImplicitDowncastTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				IntIdEntity.class,
				NameObject.class,
				PolymorphicBase.class,
				PolymorphicPropertyBase.class,
				PolymorphicPropertyMapBase.class,
				PolymorphicPropertySub1.class,
				PolymorphicPropertySub2.class,
				PolymorphicSub1.class,
				PolymorphicSub2.class
		};
	}

	@Test
	public void testQueryingSingle() {
		final Session s = openSession();
		final String base = "from PolymorphicPropertyBase p left join ";
		s.createQuery( base + "p.base b left join b.relation1 " ).getResultList();
		s.createQuery( base + "p.base b left join b.relation2 " ).getResultList();
		s.createQuery( base + "p.baseEmbeddable.embeddedRelation1 b left join b.relation1" ).getResultList();
		s.createQuery( base + "p.baseEmbeddable.embeddedRelation2 b left join b.relation2" ).getResultList();
		s.createQuery( base + "p.baseEmbeddable.embeddedBase b left join b.relation1" ).getResultList();
		s.createQuery( base + "p.baseEmbeddable.embeddedBase b left join b.relation2" ).getResultList();
	}

	@Test
	public void testQueryingMultiple() {
		final Session s = openSession();
		final String base = "from PolymorphicPropertyBase p left join ";
		s.createQuery( base + "p.base b left join b.relation1 left join b.relation2" ).getResultList();
		s.createQuery( base + "p.base b left join b.relation2 left join b.relation1" ).getResultList();
		s.createQuery( base + "p.baseEmbeddable.embeddedBase b left join b.relation1 left join b.relation2" ).getResultList();
		s.createQuery( base + "p.baseEmbeddable.embeddedBase b left join b.relation2 left join b.relation1" ).getResultList();
	}

	@Test
	public void testMultiJoinAddition1() {
		testMultiJoinAddition( "from PolymorphicPropertyBase p left join p.base b left join b.relation1" );
	}

	@Test
	public void testMultiJoinAddition2() {
		testMultiJoinAddition( "from PolymorphicPropertyBase p left join p.base b left join b.relation2" );
	}

	private void testMultiJoinAddition(String hql) {
		final HQLQueryPlan plan = sessionFactory().getQueryPlanCache().getHQLQueryPlan(
				hql,
				false,
				Collections.EMPTY_MAP
		);
		assertEquals( 1, plan.getTranslators().length );
		final QueryTranslator translator = plan.getTranslators()[0];
		final String generatedSql = translator.getSQLString();

		int sub1JoinColumnIndex = generatedSql.indexOf( ".base_sub_1" );
		assertNotEquals(
				"Generated SQL doesn't contain a join for 'base' with 'PolymorphicSub1' via 'base_sub_1':\n" + generatedSql,
				-1,
				sub1JoinColumnIndex
		);
		int sub2JoinColumnIndex = generatedSql.indexOf( ".base_sub_2" );
		assertNotEquals(
				"Generated SQL doesn't contain a join for 'base' with 'PolymorphicSub2' via 'base_sub_2':\n" + generatedSql,
				-1,
				sub2JoinColumnIndex
		);
	}


}
