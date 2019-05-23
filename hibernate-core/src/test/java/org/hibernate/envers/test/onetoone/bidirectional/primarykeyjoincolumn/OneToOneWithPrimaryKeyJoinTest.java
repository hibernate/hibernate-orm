/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.bidirectional.primarykeyjoincolumn;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.primarykeyjoincolumn.Account;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.primarykeyjoincolumn.AccountNotAuditedOwners;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.primarykeyjoincolumn.NotAuditedNoProxyPerson;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.primarykeyjoincolumn.NotAuditedProxyPerson;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.primarykeyjoincolumn.Person;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6825")
@Disabled("NPE - AbstractIdentifiableType#findNavigable - Requires PrimaryKeyJoinColumn support")
public class OneToOneWithPrimaryKeyJoinTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long personId = null;
	private Long accountId = null;
	private Long proxyPersonId = null;
	private Long noProxyPersonId = null;
	private Long accountNaoId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Account.class,
				AccountNotAuditedOwners.class,
				NotAuditedNoProxyPerson.class,
				NotAuditedProxyPerson.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Person person = new Person( "Robert" );
					Account account = new Account( "Saving" );
					person.setAccount( account );
					account.setOwner( person );
					entityManager.persist( person );
					entityManager.persist( account );

					personId = person.getPersonId();
					accountId = account.getAccountId();
				},

				// Revision 2
				entityManager -> {
					NotAuditedNoProxyPerson noProxyPerson = new NotAuditedNoProxyPerson( "Kinga" );
					NotAuditedProxyPerson proxyPerson = new NotAuditedProxyPerson( "Lukasz" );
					AccountNotAuditedOwners accountNotAuditedOwners = new AccountNotAuditedOwners( "Standard" );
					noProxyPerson.setAccount( accountNotAuditedOwners );
					proxyPerson.setAccount( accountNotAuditedOwners );
					accountNotAuditedOwners.setOwner( noProxyPerson );
					accountNotAuditedOwners.setCoOwner( proxyPerson );
					entityManager.persist( accountNotAuditedOwners );
					entityManager.persist( noProxyPerson );
					entityManager.persist( proxyPerson );

					accountNaoId = accountNotAuditedOwners.getAccountId();
					proxyPersonId = proxyPerson.getPersonId();
					noProxyPersonId = noProxyPerson.getPersonId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( Person.class, personId ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( Account.class, accountId ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( AccountNotAuditedOwners.class, accountNaoId ), contains( 2 ) );
	}

	@DynamicTest
	public void testHistoryOfPerson() {
		Person personVer1 = new Person( personId, "Robert" );
		Account accountVer1 = new Account( accountId, "Saving" );
		personVer1.setAccount( accountVer1 );
		accountVer1.setOwner( personVer1 );

		Object[] result = ((Object[]) getAuditReader().createQuery()
				.forRevisionsOfEntity( Person.class, false, true )
				.add( AuditEntity.id().eq( personId ) )
				.getResultList().get( 0 ));

		assertThat( result[0], equalTo( personVer1 ) );
		assertThat( ( (Person) result[0] ).getAccount(), equalTo( personVer1.getAccount() ) );
		assertThat( result[2], equalTo( RevisionType.ADD ) );

		assertThat( getAuditReader().find( Person.class, personId, 1 ), equalTo( personVer1 ) );
	}

	@DynamicTest
	public void testHistoryOfAccount() {
		Person personVer1 = new Person( personId, "Robert" );
		Account accountVer1 = new Account( accountId, "Saving" );
		personVer1.setAccount( accountVer1 );
		accountVer1.setOwner( personVer1 );

		Object[] result = ((Object[]) getAuditReader().createQuery()
				.forRevisionsOfEntity( Account.class, false, true )
				.add( AuditEntity.id().eq( accountId ) )
				.getResultList().get( 0 ));

		assertThat( result[0], equalTo( accountVer1 ) );
		assertThat( ( (Account) result[0] ).getOwner(), equalTo( accountVer1.getOwner() ) );
		assertThat( result[2], equalTo( RevisionType.ADD ) );

		assertThat( getAuditReader().find( Account.class, accountId, 1 ), equalTo( accountVer1 ) );
	}

	@DynamicTest
	public void testHistoryOfAccountNotAuditedOwners() {
		NotAuditedNoProxyPerson noProxyPersonVer1 = new NotAuditedNoProxyPerson( noProxyPersonId, "Kinga" );
		NotAuditedProxyPerson proxyPersonVer1 = new NotAuditedProxyPerson( proxyPersonId, "Lukasz" );
		AccountNotAuditedOwners accountNotAuditedOwnersVer1 = new AccountNotAuditedOwners( accountNaoId, "Standard" );
		noProxyPersonVer1.setAccount( accountNotAuditedOwnersVer1 );
		proxyPersonVer1.setAccount( accountNotAuditedOwnersVer1 );
		accountNotAuditedOwnersVer1.setOwner( noProxyPersonVer1 );
		accountNotAuditedOwnersVer1.setCoOwner( proxyPersonVer1 );

		Object[] result = ((Object[]) getAuditReader().createQuery()
				.forRevisionsOfEntity( AccountNotAuditedOwners.class, false, true )
				.add( AuditEntity.id().eq( accountNaoId ) )
				.getResultList()
				.get( 0 ));

		assertThat( result[0], equalTo( accountNotAuditedOwnersVer1 ) );
		assertThat( result[2], equalTo( RevisionType.ADD ) );

		final AccountNotAuditedOwners result0 = (AccountNotAuditedOwners) result[0];
		// Checking non-proxy reference
		assertThat( result0.getOwner(), equalTo( accountNotAuditedOwnersVer1.getOwner() ) );
		// Checking proxy reference
		assertThat( result0.getCoOwner(), instanceOf( HibernateProxy.class ) );
		assertThat( result0.getCoOwner().getPersonId(), equalTo( proxyPersonVer1.getPersonId() ) );

		AccountNotAuditedOwners rev2 = getAuditReader().find( AccountNotAuditedOwners.class, accountNaoId, 2 );
		assertThat( rev2, equalTo( accountNotAuditedOwnersVer1 ) );
	}
}
