/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.ids.protectedmodifier;

import java.util.Arrays;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.protectedmodifier.ProtectedConstructorEntity;
import org.hibernate.envers.test.support.domains.ids.protectedmodifier.WrappedStringId;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7934")
public class ProtectedConstructorTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private final ProtectedConstructorEntity testEntity = new ProtectedConstructorEntity(
			new WrappedStringId( "embeddedStringId" ),
			"string"
	);

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { WrappedStringId.class, ProtectedConstructorEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction( entityManager -> { entityManager.persist( testEntity ); } );
	}

	@DynamicTest
	public void testAuditEntityInstantiation() {
		assertThat(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( ProtectedConstructorEntity.class, 1 )
						.getResultList(),
				equalTo( Arrays.asList( testEntity ) )
		);
	}
}
