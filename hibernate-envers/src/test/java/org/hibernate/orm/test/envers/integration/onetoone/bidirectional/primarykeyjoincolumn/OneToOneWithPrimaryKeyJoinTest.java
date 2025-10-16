/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional.primarykeyjoincolumn;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6825")
@EnversTest
@Jpa(annotatedClasses = {Person.class, Account.class, AccountNotAuditedOwners.class, NotAuditedPerson.class})
public class OneToOneWithPrimaryKeyJoinTest {
	private Long personId = null;
	private Long accountId = null;
	private Long proxyPersonId = null;
	private Long accountNotAuditedOwnersId = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			Person person = new Person( "Robert" );
			Account account = new Account( "Saving" );
			person.setAccount( account );
			account.setOwner( person );
			em.persist( person );
			em.persist( account );

			personId = person.getPersonId();
			accountId = account.getAccountId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			NotAuditedPerson proxyPerson = new NotAuditedPerson( "Lukasz" );
			AccountNotAuditedOwners accountNotAuditedOwners = new AccountNotAuditedOwners( "Standard" );
			proxyPerson.setAccount( accountNotAuditedOwners );
			accountNotAuditedOwners.setOwner( proxyPerson );
			em.persist( accountNotAuditedOwners );
			em.persist( proxyPerson );

			accountNotAuditedOwnersId = accountNotAuditedOwners.getAccountId();
			proxyPersonId = proxyPerson.getPersonId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Person.class, personId ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Account.class, accountId ) );
			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( AccountNotAuditedOwners.class, accountNotAuditedOwnersId ) );
		} );
	}

	@Test
	public void testHistoryOfPerson(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			Person personVer1 = new Person( personId, "Robert" );
			Account accountVer1 = new Account( accountId, "Saving" );
			personVer1.setAccount( accountVer1 );
			accountVer1.setOwner( personVer1 );

			Object[] result = ((Object[]) auditReader.createQuery().forRevisionsOfEntity( Person.class, false, true )
					.add( AuditEntity.id().eq( personId ) )
					.getResultList().get( 0 ));

			assertEquals( personVer1, result[0] );
			assertEquals( personVer1.getAccount(), ((Person) result[0]).getAccount() );
			assertEquals( RevisionType.ADD, result[2] );

			assertEquals( personVer1, auditReader.find( Person.class, personId, 1 ) );
		} );
	}

	@Test
	public void testHistoryOfAccount(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			Person personVer1 = new Person( personId, "Robert" );
			Account accountVer1 = new Account( accountId, "Saving" );
			personVer1.setAccount( accountVer1 );
			accountVer1.setOwner( personVer1 );

			Object[] result = ((Object[]) auditReader.createQuery().forRevisionsOfEntity( Account.class, false, true )
					.add( AuditEntity.id().eq( accountId ) )
					.getResultList().get( 0 ));

			assertEquals( accountVer1, result[0] );
			assertEquals( accountVer1.getOwner(), ((Account) result[0]).getOwner() );
			assertEquals( RevisionType.ADD, result[2] );

			assertEquals( accountVer1, auditReader.find( Account.class, accountId, 1 ) );
		} );
	}

	@Test
	public void testHistoryOfAccountNotAuditedOwners(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			NotAuditedPerson proxyPersonVer1 = new NotAuditedPerson( proxyPersonId, "Lukasz" );
			AccountNotAuditedOwners accountNotAuditedOwnersVer1 = new AccountNotAuditedOwners( accountNotAuditedOwnersId, "Standard" );
			proxyPersonVer1.setAccount( accountNotAuditedOwnersVer1 );
			accountNotAuditedOwnersVer1.setOwner( proxyPersonVer1 );

			Object[] result = ((Object[]) auditReader.createQuery()
					.forRevisionsOfEntity( AccountNotAuditedOwners.class, false, true )
					.add( AuditEntity.id().eq( accountNotAuditedOwnersId ) )
					.getResultList()
					.get( 0 ));

			assertEquals( accountNotAuditedOwnersVer1, result[0] );
			assertEquals( RevisionType.ADD, result[2] );
			assertTrue( ((AccountNotAuditedOwners) result[0]).getOwner() instanceof HibernateProxy );
			assertEquals( proxyPersonVer1.getPersonId(), ((AccountNotAuditedOwners) result[0]).getOwner().getPersonId() );

			assertEquals( accountNotAuditedOwnersVer1, auditReader.find( AccountNotAuditedOwners.class, accountNotAuditedOwnersId, 2 ) );
		} );
	}
}
