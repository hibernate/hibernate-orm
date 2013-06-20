/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.EncapsulatedCompositeIdResultSetProcessorTest;
import org.hibernate.persister.entity.OuterJoinLoadable;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * Used to assert that "fetch graphs" between JoinWalker and LoadPlan are same.
 *
 * @author Steve Ebersole
 */
public class LoadPlanStructureAssertionTest extends BaseUnitTestCase {
	@Test
	public void testJoinedOneToOne() {
		// tests the mappings defined in org.hibernate.test.onetoone.joined.JoinedSubclassOneToOneTest
		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/onetoone/joined/Person.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

//		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Person.class ) );
		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Entity.class ) );

//		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Address.class ) );
	}

	@Test
	public void testSpecialOneToOne() {
		// tests the mappings defined in org.hibernate.test.onetoone.joined.JoinedSubclassOneToOneTest
		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/onetoone/formula/Person.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.formula.Person.class ) );
	}

	@Test
	public void testEncapsulatedCompositeId() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.Parent.class );
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.CardField.class );
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.Card.class );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();
		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( EncapsulatedCompositeIdResultSetProcessorTest.CardField.class ) );
	}

	@Test
	public void testManyToMany() {
		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/immutable/entitywithmutablecollection/inverse/ContractVariation.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();
		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.immutable.entitywithmutablecollection.Contract.class ) );

	}

	private void doCompare(SessionFactoryImplementor sf, OuterJoinLoadable persister) {
		LoadPlanStructureAssertionHelper.INSTANCE.performBasicComparison( sf, persister );
	}
}
