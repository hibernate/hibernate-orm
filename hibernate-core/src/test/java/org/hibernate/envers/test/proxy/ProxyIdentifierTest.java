/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.proxy;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.unidirectional.ManyToManyNotAuditedNullEntity;
import org.hibernate.envers.test.support.domains.manytoone.unidirectional.ExtManyToOneNotAuditedNullEntity;
import org.hibernate.envers.test.support.domains.manytoone.unidirectional.ManyToOneNotAuditedNullEntity;
import org.hibernate.envers.test.support.domains.manytoone.unidirectional.TargetNotAuditedEntity;
import org.hibernate.envers.test.support.domains.onetomany.OneToManyNotAuditedNullEntity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Eugene Goroschenya
 */
@Disabled("NYI - Inheritance support")
public class ProxyIdentifierTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private TargetNotAuditedEntity tnae1 = null;
	private ManyToOneNotAuditedNullEntity mtonane1 = null;
	private ExtManyToOneNotAuditedNullEntity emtonane1 = null;
	private ManyToManyNotAuditedNullEntity mtmnane1 = null;
	private OneToManyNotAuditedNullEntity otmnane1 = null;
	private UnversionedStrTestEntity uste1 = null;
	private UnversionedStrTestEntity uste2 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TargetNotAuditedEntity.class,
				ManyToOneNotAuditedNullEntity.class,
				UnversionedStrTestEntity.class,
				ManyToManyNotAuditedNullEntity.class,
				OneToManyNotAuditedNullEntity.class,
				ExtManyToOneNotAuditedNullEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// No revision
				entityManager -> {
					uste1 = new UnversionedStrTestEntity( "str1" );
					uste2 = new UnversionedStrTestEntity( "str2" );
					entityManager.persist( uste1 );
					entityManager.persist( uste2 );
				},

				// Revision 1
				entityManager -> {
					uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1.getId() );
					tnae1 = new TargetNotAuditedEntity( 1, "tnae1", uste1 );
					entityManager.persist( tnae1 );
				},

				// Revision 2
				entityManager -> {
					uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2.getId() );
					mtonane1 = new ManyToOneNotAuditedNullEntity( 2, "mtonane1", uste2 );
					mtmnane1 = new ManyToManyNotAuditedNullEntity( 3, "mtmnane1" );
					mtmnane1.getReferences().add( uste2 );
					otmnane1 = new OneToManyNotAuditedNullEntity( 4, "otmnane1" );
					otmnane1.getReferences().add( uste2 );
					emtonane1 = new ExtManyToOneNotAuditedNullEntity( 5, "emtonane1", uste2, "extension" );
					entityManager.persist( mtonane1 );
					entityManager.persist( mtmnane1 );
					entityManager.persist( otmnane1 );
					entityManager.persist( emtonane1 );
				},

				// Revision 3
				entityManager -> {
					entityManager.clear();

					// Remove not audited target entity, so we can verify null reference
					// when @NotFound(action = NotFoundAction.IGNORE) applied.

					ManyToOneNotAuditedNullEntity tmp1 = entityManager.find( ManyToOneNotAuditedNullEntity.class, mtonane1.getId() );
					tmp1.setReference( null );
					tmp1 = entityManager.merge( tmp1 );

					ManyToManyNotAuditedNullEntity tmp2 = entityManager.find( ManyToManyNotAuditedNullEntity.class, mtmnane1.getId() );
					tmp2.setReferences( null );
					tmp2 = entityManager.merge( tmp2 );

					OneToManyNotAuditedNullEntity tmp3 = entityManager.find( OneToManyNotAuditedNullEntity.class, otmnane1.getId() );
					tmp3.setReferences( null );
					tmp3 = entityManager.merge( tmp3 );

					ExtManyToOneNotAuditedNullEntity tmp4 = entityManager.find( ExtManyToOneNotAuditedNullEntity.class, emtonane1.getId() );
					tmp4.setReference( null );
					tmp4 = entityManager.merge( tmp4 );

					entityManager.remove( entityManager.getReference( UnversionedStrTestEntity.class, uste2.getId() ) );
				}
		);
	}

	@DynamicTest
	public void testProxyIdentifier() {
		TargetNotAuditedEntity rev1 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1.getId(), 1 );

		assertThat( rev1.getReference(), instanceOf( HibernateProxy.class ) );

		HibernateProxy proxyCreateByEnvers = (HibernateProxy) rev1.getReference();
		LazyInitializer lazyInitializer = proxyCreateByEnvers.getHibernateLazyInitializer();

		assertThat( lazyInitializer.isUninitialized(), is( true ) );
		assertThat( lazyInitializer.getIdentifier(), notNullValue() );
		assertThat( lazyInitializer.getIdentifier(), equalTo( tnae1.getId() ) );
		assertThat( lazyInitializer.isUninitialized(), is( true ) );

		assertThat( rev1.getReference().getId(), equalTo( uste1.getId() ) );
		assertThat( rev1.getReference().getStr(), equalTo( uste1.getStr() ) );
		assertThat( lazyInitializer.isUninitialized(), is( false ) );
	}

	@DynamicTest
	@TestForIssue( jiraKey = "HHH-8174" )
	public void testNullReferenceWithNotFoundActionIgnore() {
		ManyToOneNotAuditedNullEntity mtoRev2 = getAuditReader().find( ManyToOneNotAuditedNullEntity.class, mtonane1.getId(), 2 );
		assertThat( mtoRev2, equalTo( mtonane1 ) );
		assertThat( mtoRev2.getReference(), nullValue() );

		ManyToManyNotAuditedNullEntity mtmRev2 = getAuditReader().find( ManyToManyNotAuditedNullEntity.class, mtmnane1.getId(), 2 );
		assertThat( mtmRev2, equalTo( mtmnane1 ) );
		assertThat( mtmRev2.getReferences(), CollectionMatchers.isEmpty() );

		OneToManyNotAuditedNullEntity otmRev2 = getAuditReader().find( OneToManyNotAuditedNullEntity.class, otmnane1.getId(), 2 );
		assertThat( otmRev2, equalTo( otmnane1 ) );
		assertThat( otmRev2.getReferences(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	@TestForIssue( jiraKey = "HHH-8912" )
	public void testNullReferenceWithNotFoundActionIgnoreInParent() {
		ExtManyToOneNotAuditedNullEntity emtoRev2 = getAuditReader().find( ExtManyToOneNotAuditedNullEntity.class, emtonane1.getId(), 2 );
		assertThat( emtoRev2, equalTo( emtonane1 ) );
		assertThat( emtoRev2.getReference(), nullValue() );
	}
}
