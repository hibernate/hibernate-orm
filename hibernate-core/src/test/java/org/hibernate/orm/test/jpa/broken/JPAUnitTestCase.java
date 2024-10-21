/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.broken;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.LongStream;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

public class JPAUnitTestCase extends BaseEntityManagerFunctionalTestCase {


	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {ThirdParty.class, Provider.class, VoiceGroup.class, TelephoneNumber.class};
	}

	@Before
	public void setup() {
		doInJPA( this::entityManagerFactory, em -> {

			ThirdParty thirdParty = new ThirdParty();
			thirdParty.setName( "Globex Corporation" );
			em.persist( thirdParty );

			Provider provider = new Provider();
			provider.setThirdParty( thirdParty );
			em.persist( provider );

			VoiceGroup voiceGroup = new VoiceGroup();
			em.persist( voiceGroup );

			TelephoneNumber primaryNumber = new TelephoneNumber();
			primaryNumber.setNumber( "4065551234" );
			primaryNumber.setProvider( provider );
			primaryNumber.setVoiceGroup( voiceGroup );
			em.persist( primaryNumber );

			voiceGroup.setPrimaryNumber( primaryNumber );

			LongStream.rangeClosed( 4065551235L, 4065551255L ).forEach( value -> {
				TelephoneNumber telephoneNumber = new TelephoneNumber();
				telephoneNumber.setNumber( String.valueOf( value ) );
				telephoneNumber.setProvider( provider );
				telephoneNumber.setVoiceGroup( voiceGroup );
				em.persist( telephoneNumber );
			} );
		} );
	}

	@Test
	public void testThatPasses() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		VoiceGroup voiceGroup = em.find( VoiceGroup.class, 1,
				Map.of( "jakarta.persistence.fetchgraph", em.getEntityGraph( "voiceGroup.graph" ) ) );
		allNumbersWithThirdPartyFetch( em, voiceGroup ).forEach( telephoneNumber -> Objects.requireNonNull( telephoneNumber.getProvider().getName() ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testThatFails() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		VoiceGroup voiceGroup = em.find( VoiceGroup.class, 1,
				Map.of( "jakarta.persistence.fetchgraph", em.getEntityGraph( "voiceGroup.graph" ) ) );
		allNumbersWithoutThirdPartyFetch( em, voiceGroup ).forEach( telephoneNumber -> Objects.requireNonNull( telephoneNumber.getProvider().getName() ) );
		em.getTransaction().commit();
		em.close();
	}


	private List<TelephoneNumber> allNumbersWithoutThirdPartyFetch(EntityManager em, VoiceGroup voiceGroup) {
		CriteriaQuery<TelephoneNumber> query = em.getCriteriaBuilder().createQuery( TelephoneNumber.class );
		Root<TelephoneNumber> root = query.from( TelephoneNumber.class );
		root.fetch( "provider" );
		query.where( em.getCriteriaBuilder().equal( root.get( "voiceGroup" ), voiceGroup ) );
		return em.createQuery( query ).getResultList();
	}

	private List<TelephoneNumber> allNumbersWithThirdPartyFetch(EntityManager em, VoiceGroup voiceGroup) {
		CriteriaQuery<TelephoneNumber> query = em.getCriteriaBuilder().createQuery( TelephoneNumber.class );
		Root<TelephoneNumber> root = query.from( TelephoneNumber.class );
		root.fetch( "provider" ).fetch( "thirdParty" );
		query.where( em.getCriteriaBuilder().equal( root.get( "voiceGroup" ), voiceGroup ) );
		return em.createQuery( query ).getResultList();
	}
}
