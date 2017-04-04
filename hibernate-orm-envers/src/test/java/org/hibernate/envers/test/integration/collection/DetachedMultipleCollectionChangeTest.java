/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.MultipleCollectionEntity;
import org.hibernate.envers.test.entities.collection.MultipleCollectionRefEntity1;
import org.hibernate.envers.test.entities.collection.MultipleCollectionRefEntity2;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test the audit history of a detached entity with multiple collections that is
 * merged back into the persistence context.
 *
 * @author Erik-Berndt Scheper
 */
@TestForIssue(jiraKey = "HHH-6349")
@SkipForDialect(value = Oracle8iDialect.class,
				comment = "Oracle does not support identity key generation")
public class DetachedMultipleCollectionChangeTest extends BaseEnversJPAFunctionalTestCase {
	private TransactionManager tm = null;

	private Long mceId1 = null;
	private Long re1Id1 = null;
	private Long re1Id2 = null;
	private Long re1Id3 = null;
	private Long re2Id1 = null;
	private Long re2Id2 = null;
	private Long re2Id3 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MultipleCollectionEntity.class,
				MultipleCollectionRefEntity1.class,
				MultipleCollectionRefEntity2.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
		tm = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
	}

	@Test
	@Priority(10)
	public void initData() throws Exception {
		EntityManager em;
		MultipleCollectionEntity mce;
		MultipleCollectionRefEntity1 re1_1, updatedRe1_1, re1_2, re1_3;
		MultipleCollectionRefEntity2 re2_1, updatedRe2_1, re2_2, re2_3;

		tm.begin();
		try {
			em = createIsolatedEntityManager();
			em.joinTransaction();
			mce = new MultipleCollectionEntity();
			mce.setText( "MultipleCollectionEntity-1" );
			em.persist( mce );
			mceId1 = mce.getId();
		}
		finally {
			tryCommit( tm );
		}
		assertNotNull( mceId1 );

		tm.begin();
		try {
			em = createIsolatedEntityManager();
			em.joinTransaction();

			re1_1 = new MultipleCollectionRefEntity1();
			re1_1.setText( "MultipleCollectionRefEntity1-1" );
			re1_1.setMultipleCollectionEntity( mce );

			re1_2 = new MultipleCollectionRefEntity1();
			re1_2.setText( "MultipleCollectionRefEntity1-2" );
			re1_2.setMultipleCollectionEntity( mce );

			mce.addRefEntity1( re1_1 );
			mce.addRefEntity1( re1_2 );

			re2_1 = new MultipleCollectionRefEntity2();
			re2_1.setText( "MultipleCollectionRefEntity2-1" );
			re2_1.setMultipleCollectionEntity( mce );

			re2_2 = new MultipleCollectionRefEntity2();
			re2_2.setText( "MultipleCollectionRefEntity2-2" );
			re2_2.setMultipleCollectionEntity( mce );

			mce.addRefEntity2( re2_1 );
			mce.addRefEntity2( re2_2 );

			mce = em.merge( mce );
		}
		finally {
			tryCommit( tm );
		}
		for ( MultipleCollectionRefEntity1 refEnt1 : mce.getRefEntities1() ) {
			if ( refEnt1.equals( re1_1 ) ) {
				re1Id1 = refEnt1.getId();
			}
			else if ( refEnt1.equals( re1_2 ) ) {
				re1Id2 = refEnt1.getId();
			}
			else {
				throw new IllegalStateException( "unexpected instance" );
			}
		}
		for ( MultipleCollectionRefEntity2 refEnt2 : mce.getRefEntities2() ) {
			if ( refEnt2.equals( re2_1 ) ) {
				re2Id1 = refEnt2.getId();
			}
			else if ( refEnt2.equals( re2_2 ) ) {
				re2Id2 = refEnt2.getId();
			}
			else {
				throw new IllegalStateException( "unexpected instance" );
			}
		}
		assertNotNull( re1Id1 );
		assertNotNull( re1Id2 );
		assertNotNull( re2Id1 );
		assertNotNull( re2Id2 );

		tm.begin();
		try {
			em = createIsolatedEntityManager();
			em.joinTransaction();

			assertEquals( 2, mce.getRefEntities1().size() );

			mce.removeRefEntity1( re1_2 );
			assertEquals( 1, mce.getRefEntities1().size() );

			updatedRe1_1 = mce.getRefEntities1().get( 0 );
			assertEquals( re1_1, updatedRe1_1 );
			updatedRe1_1.setText( "MultipleCollectionRefEntity1-1-updated" );

			re1_3 = new MultipleCollectionRefEntity1();
			re1_3.setText( "MultipleCollectionRefEntity1-3" );
			re1_3.setMultipleCollectionEntity( mce );
			mce.addRefEntity1( re1_3 );
			assertEquals( 2, mce.getRefEntities1().size() );

			assertEquals( 2, mce.getRefEntities2().size() );

			mce.removeRefEntity2( re2_2 );
			assertEquals( 1, mce.getRefEntities2().size() );

			updatedRe2_1 = mce.getRefEntities2().get( 0 );
			assertEquals( re2_1, updatedRe2_1 );
			updatedRe2_1.setText( "MultipleCollectionRefEntity2-1-updated" );

			re2_3 = new MultipleCollectionRefEntity2();
			re2_3.setText( "MultipleCollectionRefEntity2-3" );
			re2_3.setMultipleCollectionEntity( mce );
			mce.addRefEntity2( re2_3 );
			assertEquals( 2, mce.getRefEntities2().size() );

			mce = em.merge( mce );

		}
		finally {
			tryCommit( tm );
		}
		for ( MultipleCollectionRefEntity1 adres : mce.getRefEntities1() ) {
			if ( adres.equals( re1_3 ) ) {
				re1Id3 = adres.getId();
			}
		}
		for ( MultipleCollectionRefEntity2 partner : mce.getRefEntities2() ) {
			if ( partner.equals( re2_3 ) ) {
				re2Id3 = partner.getId();
			}
		}
		assertNotNull( re1Id3 );
		assertNotNull( re2Id3 );
	}

	@Test
	public void testRevisionsCounts() throws Exception {
		List<Number> mceId1Revs = getAuditReader().getRevisions( MultipleCollectionEntity.class, mceId1 );
		List<Number> re1Id1Revs = getAuditReader().getRevisions( MultipleCollectionRefEntity1.class, re1Id1 );
		List<Number> re1Id2Revs = getAuditReader().getRevisions( MultipleCollectionRefEntity1.class, re1Id2 );
		List<Number> re1Id3Revs = getAuditReader().getRevisions( MultipleCollectionRefEntity1.class, re1Id3 );
		List<Number> re2Id1Revs = getAuditReader().getRevisions( MultipleCollectionRefEntity2.class, re2Id1 );
		List<Number> re2Id2Revs = getAuditReader().getRevisions( MultipleCollectionRefEntity2.class, re2Id2 );
		List<Number> re2Id3Revs = getAuditReader().getRevisions( MultipleCollectionRefEntity2.class, re2Id3 );

		assertEquals( Arrays.asList( 1, 2, 3 ), mceId1Revs );
		assertEquals( Arrays.asList( 2, 3 ), re1Id1Revs );
		assertEquals( Arrays.asList( 2, 3 ), re1Id2Revs );
		assertEquals( Arrays.asList( 3 ), re1Id3Revs );
		assertEquals( Arrays.asList( 2, 3 ), re2Id1Revs );
		assertEquals( Arrays.asList( 2, 3 ), re2Id2Revs );
		assertEquals( Arrays.asList( 3 ), re2Id3Revs );
	}

	@Test
	public void testAuditJoinTable() throws Exception {
		List<AuditJoinTableInfo> mceRe1AuditJoinTableInfos = getAuditJoinTableRows(
				"MCE_RE1_AUD", "MCE_ID",
				"aud.originalId.MultipleCollectionEntity_id", "RE1_ID",
				"aud.originalId.refEntities1_id", "aud.originalId.REV",
				"aud.originalId.REV.id", "aud.REVTYPE"
		);
		List<AuditJoinTableInfo> mceRe2AuditJoinTableInfos = getAuditJoinTableRows(
				"MCE_RE2_AUD", "MCE_ID",
				"aud.originalId.MultipleCollectionEntity_id", "RE2_ID",
				"aud.originalId.refEntities2_id", "aud.originalId.REV",
				"aud.originalId.REV.id", "aud.REVTYPE"
		);

		assertEquals( 4, mceRe1AuditJoinTableInfos.size() );
		assertEquals( 4, mceRe2AuditJoinTableInfos.size() );

		SequenceIdRevisionEntity rev2 = new SequenceIdRevisionEntity();
		rev2.setId( 2 );
		SequenceIdRevisionEntity rev3 = new SequenceIdRevisionEntity();
		rev3.setId( 3 );

		assertEquals(
				new AuditJoinTableInfo( "MCE_RE1_AUD", rev2, RevisionType.ADD, "MCE_ID", 1L, "RE1_ID", 1L ),
				mceRe1AuditJoinTableInfos.get( 0 )
		);
		assertEquals(
				new AuditJoinTableInfo( "MCE_RE1_AUD", rev2, RevisionType.ADD, "MCE_ID", 1L, "RE1_ID", 2L ),
				mceRe1AuditJoinTableInfos.get( 1 )
		);
		assertEquals(
				new AuditJoinTableInfo( "MCE_RE1_AUD", rev3, RevisionType.DEL, "MCE_ID", 1L, "RE1_ID", 2L ),
				mceRe1AuditJoinTableInfos.get( 2 )
		);
		assertEquals(
				new AuditJoinTableInfo( "MCE_RE1_AUD", rev3, RevisionType.ADD, "MCE_ID", 1L, "RE1_ID", 3L ),
				mceRe1AuditJoinTableInfos.get( 3 )
		);

		assertEquals(
				new AuditJoinTableInfo( "MCE_RE2_AUD", rev2, RevisionType.ADD, "MCE_ID", 1L, "RE2_ID", 1L ),
				mceRe2AuditJoinTableInfos.get( 0 )
		);
		assertEquals(
				new AuditJoinTableInfo( "MCE_RE2_AUD", rev2, RevisionType.ADD, "MCE_ID", 1L, "RE2_ID", 2L ),
				mceRe2AuditJoinTableInfos.get( 1 )
		);
		assertEquals(
				new AuditJoinTableInfo( "MCE_RE2_AUD", rev3, RevisionType.DEL, "MCE_ID", 1L, "RE2_ID", 2L ),
				mceRe2AuditJoinTableInfos.get( 2 )
		);
		assertEquals(
				new AuditJoinTableInfo( "MCE_RE2_AUD", rev3, RevisionType.ADD, "MCE_ID", 1L, "RE2_ID", 3L ),
				mceRe2AuditJoinTableInfos.get( 3 )
		);
	}

	private List<AuditJoinTableInfo> getAuditJoinTableRows(
			String middleEntityName, String joinColumnIdName,
			String joinColumnIdProp, String inverseJoinColumnIdName,
			String inverseJoinColumnIdProp, String revProp, String revIdProp,
			String revTypeProp) throws Exception {
		StringBuilder qryBuilder = new StringBuilder( "select " );
		qryBuilder.append( "aud " );
		qryBuilder.append( ", " ).append( joinColumnIdProp ).append( " as joinColumnId" );
		qryBuilder.append( ", " ).append( inverseJoinColumnIdProp ).append( " as inverseJoinColumnId" );
		qryBuilder.append( ", " ).append( revProp ).append( " as rev" );
		qryBuilder.append( ", " ).append( revIdProp ).append( " as revId" );
		qryBuilder.append( ", " ).append( revTypeProp ).append( " as revType" );
		qryBuilder.append( " from " ).append( middleEntityName ).append( " aud " );
		qryBuilder.append( " order by joinColumnId asc, inverseJoinColumnId asc, revId asc" );

		String query = qryBuilder.toString();

		EntityManager em = createIsolatedEntityManager();
		Query qry = em.createQuery( query );

		@SuppressWarnings("unchecked")
		List<Object[]> auditJoinTableRows = qry.getResultList();
		List<AuditJoinTableInfo> result = new ArrayList<AuditJoinTableInfo>( auditJoinTableRows.size() );

		for ( Object[] auditJoinTableRow : auditJoinTableRows ) {
			Long joinColumnId = (Long) auditJoinTableRow[1];
			Long inverseJoinColumnId = (Long) auditJoinTableRow[2];
			SequenceIdRevisionEntity rev = (SequenceIdRevisionEntity) auditJoinTableRow[3];
			RevisionType revType = (RevisionType) auditJoinTableRow[5];

			AuditJoinTableInfo info = new AuditJoinTableInfo(
					middleEntityName, rev, revType, joinColumnIdName, joinColumnId,
					inverseJoinColumnIdName, inverseJoinColumnId
			);
			result.add( info );
		}

		return result;
	}

	private static class AuditJoinTableInfo {
		private final String name;
		private final Integer revId;
		private final RevisionType revType;
		private final String joinColumnName;
		private final Long joinColumnId;
		private final String inverseJoinColumnName;
		private final Long inverseJoinColumnId;

		private AuditJoinTableInfo(
				String name, SequenceIdRevisionEntity rev,
				RevisionType revType, String joinColumnName, Long joinColumnId,
				String inverseJoinColumnName, Long inverseJoinColumnId) {
			this.name = name;
			this.revId = rev.getId();
			this.revType = revType;
			this.joinColumnName = joinColumnName;
			this.joinColumnId = joinColumnId;
			this.inverseJoinColumnName = inverseJoinColumnName;
			this.inverseJoinColumnId = inverseJoinColumnId;
		}

		@Override
		public String toString() {
			return "AuditJoinTableInfo [name=" + name + ", revId=" + revId
					+ ", revType=" + revType + ", " + joinColumnName + "="
					+ joinColumnId + ", " + inverseJoinColumnName + "="
					+ inverseJoinColumnId + "]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !(o instanceof AuditJoinTableInfo) ) {
				return false;
			}

			AuditJoinTableInfo that = (AuditJoinTableInfo) o;

			if ( inverseJoinColumnId != null ?
					!inverseJoinColumnId.equals( that.inverseJoinColumnId ) :
					that.inverseJoinColumnId != null ) {
				return false;
			}
			if ( joinColumnId != null ? !joinColumnId.equals( that.joinColumnId ) : that.joinColumnId != null ) {
				return false;
			}
			if ( name != null ? !name.equals( that.name ) : that.name != null ) {
				return false;
			}
			if ( revId != null ? !revId.equals( that.revId ) : that.revId != null ) {
				return false;
			}
			if ( revType != that.revType ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (revId != null ? revId.hashCode() : 0);
			result = 31 * result + (revType != null ? revType.hashCode() : 0);
			result = 31 * result + (joinColumnId != null ? joinColumnId.hashCode() : 0);
			result = 31 * result + (inverseJoinColumnId != null ? inverseJoinColumnId.hashCode() : 0);
			return result;
		}
	}

	public static void tryCommit(TransactionManager tm) throws Exception {
		if ( tm.getStatus() == Status.STATUS_MARKED_ROLLBACK ) {
			tm.rollback();
		}
		else {
			tm.commit();
		}
	}
}