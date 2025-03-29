/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional.primarykeyjoincolumn;

import java.util.Arrays;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

import jakarta.persistence.EntityManager;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6825")
public class OneToOneWithPrimaryKeyJoinTest extends BaseEnversJPAFunctionalTestCase {
	private Long personId = null;
	private Long accountId = null;
	private Long proxyPersonId = null;
	private Long accountNotAuditedOwnersId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Account.class,
				AccountNotAuditedOwners.class,
				NotAuditedPerson.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		Person person = new Person( "Robert" );
		Account account = new Account( "Saving" );
		person.setAccount( account );
		account.setOwner( person );
		em.persist( person );
		em.persist( account );
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		NotAuditedPerson proxyPerson = new NotAuditedPerson( "Lukasz" );
		AccountNotAuditedOwners accountNotAuditedOwners = new AccountNotAuditedOwners( "Standard" );
		proxyPerson.setAccount( accountNotAuditedOwners );
		accountNotAuditedOwners.setOwner( proxyPerson );
		em.persist( accountNotAuditedOwners );
		em.persist( proxyPerson );
		em.getTransaction().commit();

		personId = person.getPersonId();
		accountId = account.getAccountId();
		accountNotAuditedOwnersId = accountNotAuditedOwners.getAccountId();
		proxyPersonId = proxyPerson.getPersonId();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Person.class, personId ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Account.class, accountId ) );
		Assert.assertEquals(
				Arrays.asList( 2 ), getAuditReader().getRevisions(
				AccountNotAuditedOwners.class,
				accountNotAuditedOwnersId
		)
		);
	}

	@Test
	public void testHistoryOfPerson() {
		Person personVer1 = new Person( personId, "Robert" );
		Account accountVer1 = new Account( accountId, "Saving" );
		personVer1.setAccount( accountVer1 );
		accountVer1.setOwner( personVer1 );

		Object[] result = ((Object[]) getAuditReader().createQuery().forRevisionsOfEntity( Person.class, false, true )
				.add( AuditEntity.id().eq( personId ) )
				.getResultList().get( 0 ));

		Assert.assertEquals( personVer1, result[0] );
		Assert.assertEquals( personVer1.getAccount(), ((Person) result[0]).getAccount() );
		Assert.assertEquals( RevisionType.ADD, result[2] );

		Assert.assertEquals( personVer1, getAuditReader().find( Person.class, personId, 1 ) );
	}

	@Test
	public void testHistoryOfAccount() {
		Person personVer1 = new Person( personId, "Robert" );
		Account accountVer1 = new Account( accountId, "Saving" );
		personVer1.setAccount( accountVer1 );
		accountVer1.setOwner( personVer1 );

		Object[] result = ((Object[]) getAuditReader().createQuery().forRevisionsOfEntity( Account.class, false, true )
				.add( AuditEntity.id().eq( accountId ) )
				.getResultList().get( 0 ));

		Assert.assertEquals( accountVer1, result[0] );
		Assert.assertEquals( accountVer1.getOwner(), ((Account) result[0]).getOwner() );
		Assert.assertEquals( RevisionType.ADD, result[2] );

		Assert.assertEquals( accountVer1, getAuditReader().find( Account.class, accountId, 1 ) );
	}

	@Test
	public void testHistoryOfAccountNotAuditedOwners() {
		NotAuditedPerson proxyPersonVer1 = new NotAuditedPerson( proxyPersonId, "Lukasz" );
		AccountNotAuditedOwners accountNotAuditedOwnersVer1 = new AccountNotAuditedOwners(
				accountNotAuditedOwnersId,
				"Standard"
		);
		proxyPersonVer1.setAccount( accountNotAuditedOwnersVer1 );
		accountNotAuditedOwnersVer1.setOwner( proxyPersonVer1 );

		Object[] result = ((Object[]) getAuditReader().createQuery()
				.forRevisionsOfEntity( AccountNotAuditedOwners.class, false, true )
				.add( AuditEntity.id().eq( accountNotAuditedOwnersId ) )
				.getResultList()
				.get( 0 ));

		Assert.assertEquals( accountNotAuditedOwnersVer1, result[0] );
		Assert.assertEquals( RevisionType.ADD, result[2] );
		Assert.assertTrue( ((AccountNotAuditedOwners) result[0]).getOwner() instanceof HibernateProxy );
		Assert.assertEquals(
				proxyPersonVer1.getPersonId(),
				((AccountNotAuditedOwners) result[0]).getOwner().getPersonId()
		);

		Assert.assertEquals(
				accountNotAuditedOwnersVer1, getAuditReader().find(
				AccountNotAuditedOwners.class,
				accountNotAuditedOwnersId,
				2
		)
		);
	}
}
