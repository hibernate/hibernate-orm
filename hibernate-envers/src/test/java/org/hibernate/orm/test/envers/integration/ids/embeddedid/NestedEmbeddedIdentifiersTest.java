/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-13361")
public class NestedEmbeddedIdentifiersTest extends BaseEnversJPAFunctionalTestCase {

	private OwnerOfRelationCodeId id;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] { OwnerOfRelationCode.class, CompositeEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1, test insert
		final OwnerOfRelationCode owner = doInJPA( this::entityManagerFactory, session -> {
			CompositeEntity compositeEntity = new CompositeEntity();
			compositeEntity.setFirstCode( "firstCode" );
			compositeEntity.setSecondCode( "secondCode" );
			session.persist( compositeEntity );

			OwnerOfRelationCode ownerEntity = new OwnerOfRelationCode();
			ownerEntity.setCompositeEntity( compositeEntity );
			ownerEntity.setSecondIdentifier( "secondIdentifier" );

			session.persist( ownerEntity );
			return ownerEntity;
		} );

		this.id = owner.getCodeObject();

		// Revision 2, test update
		doInJPA( this::entityManagerFactory, session -> {
			OwnerOfRelationCode ownerEntity = session.find( OwnerOfRelationCode.class, id );
			ownerEntity.setDescription( "first description" );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( OwnerOfRelationCode.class, id ) );
	}

	@Test
	public void testIdentifierAtRevision1() {
		// select e__
		//   from org.hibernate.orm.test.envers.integration.ids.embeddedid.OwnerOfRelationCode_AUD e__
		//  where e__.originalId.REV.id = (select max(e2__.originalId.REV.id)
		//                                   from org.hibernate.orm.test.envers.integration.ids.embeddedid.OwnerOfRelationCode_AUD e2__
		//                                  where e2__.originalId.REV.id <= :revision
		//                                    and e__.originalId.compositeEntity = e2__.originalId.compositeEntity
		//                                    and e__.originalId.secondIdentifier = e2__.originalId.secondIdentifier)
		//    and e__.REVTYPE <> :_p0
		//    and e__.originalId.compositeEntity_firstCode = :_p1
		//    and e__.originalId.compositeEntity_secondCode = :_p2
		//    and e__.originalId.secondIdentifier = :_p3
		//
		// select e__
		//   from org.hibernate.orm.test.envers.integration.ids.embeddedid.OwnerOfRelationCode_AUD e__
		//  where e__.originalId.REV.id = (select max(e2__.originalId.REV.id)
		//                                   from org.hibernate.orm.test.envers.integration.ids.embeddedid.OwnerOfRelationCode_AUD e2__
		//                                  where e2__.originalId.REV.id <= :revision
		//                                    and e__.originalId.compositeEntity_firstCode = e2__.originalId.compositeEntity_firstCode
		//                                    and e__.originalId.compositeEntity_secondCode = e2__.originalId.compositeEntity_secondCode
		//                                    and e__.originalId.secondIdentifier = e2__.originalId.secondIdentifier)
		//    and e__.REVTYPE <> :_p0
		//    and e__.originalId.compositeEntity_firstCode = :_p1
		//    and e__.originalId.compositeEntity_secondCode = :_p2
		//    and e__.originalId.secondIdentifier = :_p3
		final OwnerOfRelationCode rev1 = getAuditReader().find( OwnerOfRelationCode.class, id, 1 );
		System.out.println( rev1 );
		assertEquals( rev1.getCodeObject().getSecondIdentifier(), "secondIdentifier" );
		assertEquals( rev1.getCodeObject().getCompositeEntity().getFirstCode(), "firstCode" );
		assertEquals( rev1.getCodeObject().getCompositeEntity().getSecondCode(), "secondCode" );
		assertNull( rev1.getDescription() );
	}

	@Test
	public void testIdentifierAtRevision2() {
		final OwnerOfRelationCode rev2 = getAuditReader().find( OwnerOfRelationCode.class, id, 2 );
		System.out.println( rev2 );
		assertEquals( rev2.getCodeObject().getSecondIdentifier(), "secondIdentifier" );
		assertEquals( rev2.getCodeObject().getCompositeEntity().getFirstCode(), "firstCode" );
		assertEquals( rev2.getCodeObject().getCompositeEntity().getSecondCode(), "secondCode" );
		assertEquals( rev2.getDescription(), "first description" );
	}
}
