/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class SubselectTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "subselect/Beings.hbm.xml" };
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEntitySubselect() {
		inTransaction(
				s -> {
					Human gavin = new Human();
					gavin.setName( "gavin" );
					gavin.setSex( 'M' );
					gavin.setAddress( "Melbourne, Australia" );
					Alien x23y4 = new Alien();
					x23y4.setIdentity( "x23y4$$hu%3" );
					x23y4.setPlanet( "Mars" );
					x23y4.setSpecies( "martian" );
					s.persist( gavin );
					s.persist( x23y4 );
					s.flush();
					List<Being> beings = (List<Being>) s.createQuery( "from Being" ).list();
					for ( Being being : beings ) {
						assertNotNull( being.getLocation() );
						assertNotNull( being.getIdentity() );
						assertNotNull( being.getSpecies() );
					}
					s.clear();
					sessionFactory().getCache().evictEntityData( Being.class );
					Being gav = s.get( Being.class, gavin.getId() );
					assertEquals( gav.getLocation(), gavin.getAddress() );
					assertEquals( gav.getSpecies(), "human" );
					assertEquals( gav.getIdentity(), gavin.getName() );
					s.clear();
					//test the <synchronized> tag:
					gavin = s.get( Human.class, gavin.getId() );
					gavin.setAddress( "Atlanta, GA" );
					gav = (Being) s.createQuery( "from Being b where b.location like '%GA%'" ).uniqueResult();
					assertEquals( gav.getLocation(), gavin.getAddress() );
					s.remove( gavin );
					s.remove( x23y4 );
					assertTrue( s.createQuery( "from Being" ).list().isEmpty() );

				}
		);
	}

	@Test
	public void testCustomColumnReadAndWrite() {
		inTransaction(
				s -> {
					final double HUMAN_INCHES = 73;
					final double ALIEN_INCHES = 931;
					final double HUMAN_CENTIMETERS = HUMAN_INCHES * 2.54d;
					final double ALIEN_CENTIMETERS = ALIEN_INCHES * 2.54d;
					Human gavin = new Human();
					gavin.setName( "gavin" );
					gavin.setSex( 'M' );
					gavin.setAddress( "Melbourne, Australia" );
					gavin.setHeightInches( HUMAN_INCHES );
					Alien x23y4 = new Alien();
					x23y4.setIdentity( "x23y4$$hu%3" );
					x23y4.setPlanet( "Mars" );
					x23y4.setSpecies( "martian" );
					x23y4.setHeightInches( ALIEN_INCHES );
					s.persist( gavin );
					s.persist( x23y4 );
					s.flush();

					// Test value conversion during insert
					// Value returned by Oracle native query is a Types.NUMERIC, which is mapped to a BigDecimalType;
					// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
					Double humanHeightViaSql =
							( (Number) s.createNativeQuery( "select height_centimeters from humans" )
									.uniqueResult() ).doubleValue();
					assertEquals( HUMAN_CENTIMETERS, humanHeightViaSql, 0.01d );
					Double alienHeightViaSql =
							( (Number) s.createNativeQuery( "select height_centimeters from aliens" )
									.uniqueResult() ).doubleValue();
					assertEquals( ALIEN_CENTIMETERS, alienHeightViaSql, 0.01d );
					s.clear();

					// Test projection
					Double heightViaHql = (Double) s.createQuery(
							"select heightInches from Being b where b.identity = 'gavin'" ).uniqueResult();
					assertEquals( HUMAN_INCHES, heightViaHql, 0.01d );

					// Test restriction and entity load via criteria

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Being> criteria = criteriaBuilder.createQuery( Being.class );
					Root<Being> root = criteria.from( Being.class );
					criteria.where( criteriaBuilder.between( root.get( "heightInches" ),HUMAN_INCHES - 0.01d, HUMAN_INCHES + 0.01d  ) );

					Being b = s.createQuery( criteria ).uniqueResult();

//					Being b = (Being) s.createCriteria( Being.class )
//							.add( Restrictions.between( "heightInches", HUMAN_INCHES - 0.01d, HUMAN_INCHES + 0.01d ) )
//							.uniqueResult();
					assertEquals( HUMAN_INCHES, b.getHeightInches(), 0.01d );

					// Test predicate and entity load via HQL
					b = (Being) s.createQuery( "from Being b where b.heightInches between ?1 and ?2" )
							.setParameter( 1, ALIEN_INCHES - 0.01d )
							.setParameter( 2, ALIEN_INCHES + 0.01d )
							.uniqueResult();
					assertEquals( ALIEN_INCHES, b.getHeightInches(), 0.01d );
					s.remove( gavin );
					s.remove( x23y4 );
				}
		);
	}

}
