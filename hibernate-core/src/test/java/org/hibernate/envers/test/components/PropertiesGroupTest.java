/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.UniquePropsEntity;
import org.hibernate.envers.test.support.domains.components.UniquePropsNotAuditedEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6636")
@Disabled("NYI - SyntheticProperty support")
public class PropertiesGroupTest extends EnversSessionFactoryBasedFunctionalTest {
	private UniquePropsEntity entityRev1 = null;
	private UniquePropsNotAuditedEntity entityNotAuditedRev2 = null;

	@Override
	protected String[] getMappings() {
		return new String[] {
				"components/UniquePropsEntity.hbm.xml",
				"components/UniquePropsNotAuditedEntity.hbm.xml"
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				session -> {
					UniquePropsEntity ent = new UniquePropsEntity();
					ent.setData1( "data1" );
					ent.setData2( "data2" );
					session.persist( ent );

					entityRev1 = new UniquePropsEntity( ent.getId(), ent.getData1(), ent.getData2() );
				},

				// Revision 2
				session -> {
					UniquePropsNotAuditedEntity entNotAud = new UniquePropsNotAuditedEntity();
					entNotAud.setData1( "data3" );
					entNotAud.setData2( "data4" );
					session.persist( entNotAud );

					entityNotAuditedRev2 = new UniquePropsNotAuditedEntity( entNotAud.getId(), entNotAud.getData1(), null );
				}
		);
	}

	@DynamicTest
	public void testAuditTableColumns() {
		final EntityTypeDescriptor descriptor1 = getAuditEntityDescriptor( UniquePropsEntity.class );
		assertThat( descriptor1.getPrimaryTable().getColumn( "DATA1" ), notNullValue() );
		assertThat( descriptor1.getPrimaryTable().getColumn( "DATA2" ), notNullValue() );

		final EntityTypeDescriptor descriptor2 = getAuditEntityDescriptor( UniquePropsNotAuditedEntity.class );
		assertThat( descriptor2.getPrimaryTable().getColumn( "DATA1" ), notNullValue() );
		assertThat( descriptor2.getPrimaryTable().getColumn( "DATA2" ), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfUniquePropsEntity() {
		assertThat( getAuditReader().find( UniquePropsEntity.class, entityRev1.getId(), 1 ), equalTo( entityRev1 ) );
	}

	@DynamicTest
	public void testHistoryOfUniquePropsNotAuditedEntity() {
		assertThat(
				getAuditReader().find( UniquePropsNotAuditedEntity.class, entityNotAuditedRev2.getId(), 2 ),
				equalTo( entityNotAuditedRev2 )
		);
	}
}
