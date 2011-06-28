package org.hibernate.envers.test.integration.collection;

import static org.hibernate.envers.test.EnversTestingJtaBootstrap.tryCommit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.EnversTestingJtaBootstrap;
import org.hibernate.envers.test.Priority;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * Test the audit history of a detached entity with multiple collections that is
 * merged back into the persistence context.
 * 
 * @author Erik-Berndt Scheper
 */
public class DetachedMultipleCollectionChangeTest extends AbstractEntityTest {

	public static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			DetachedMultipleCollectionChangeTest.class.getName());

	private TransactionManager tm;

	private Long mceId1;
	private Long re1Id1;
	private Long re1Id2;
	private Long re1Id3;
	private Long re2Id1;
	private Long re2Id2;
	private Long re2Id3;

	public void configure(Ejb3Configuration cfg) {
		cfg.addAnnotatedClass(MultipleCollectionEntity.class);
		cfg.addAnnotatedClass(MultipleCollectionRefEntity1.class);
		cfg.addAnnotatedClass(MultipleCollectionRefEntity2.class);
	}

	@Override
	public void addConfigurationProperties(Properties configuration) {
		super.addConfigurationProperties(configuration);
		tm = EnversTestingJtaBootstrap.updateConfigAndCreateTM(configuration);
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
			newEntityManager();
			em = getEntityManager();
			em.joinTransaction();
			mce = new MultipleCollectionEntity();
			mce.setText("MultipleCollectionEntity-1");
			em.persist(mce);
			mceId1 = mce.getId();

		} finally {
			tryCommit(tm);
		}

		assertNotNull(mceId1);

		//

		tm.begin();

		try {
			newEntityManager();
			em = getEntityManager();
			em.joinTransaction();

			// mce = em.find(MultipleCollectionEntity.class, mceId1);
			// mce = em.merge(mce);

			re1_1 = new MultipleCollectionRefEntity1();
			re1_1.setText("MultipleCollectionRefEntity1-1");
			re1_1.setMultipleCollectionEntity(mce);

			re1_2 = new MultipleCollectionRefEntity1();
			re1_2.setText("MultipleCollectionRefEntity1-2");
			re1_2.setMultipleCollectionEntity(mce);

			mce.addRefEntity1(re1_1);
			mce.addRefEntity1(re1_2);

			re2_1 = new MultipleCollectionRefEntity2();
			re2_1.setText("MultipleCollectionRefEntity2-1");
			re2_1.setMultipleCollectionEntity(mce);

			re2_2 = new MultipleCollectionRefEntity2();
			re2_2.setText("MultipleCollectionRefEntity2-2");
			re2_2.setMultipleCollectionEntity(mce);

			mce.addRefEntity2(re2_1);
			mce.addRefEntity2(re2_2);

			mce = em.merge(mce);

		} finally {
			tryCommit(tm);
		}

		// re1Id1 = re1_1.getId();
		// re1Id2 = re1_2.getId();
		// re2Id1 = re2_1.getId();
		// re2Id2 = re2_2.getId();

		for (MultipleCollectionRefEntity1 refEnt1 : mce.getRefEntities1()) {
			if (refEnt1.equals(re1_1)) {
				re1Id1 = refEnt1.getId();
			} else if (refEnt1.equals(re1_2)) {
				re1Id2 = refEnt1.getId();
			} else {
				throw new IllegalStateException("unexpected instance");
			}
		}

		for (MultipleCollectionRefEntity2 refEnt2 : mce.getRefEntities2()) {
			if (refEnt2.equals(re2_1)) {
				re2Id1 = refEnt2.getId();
			} else if (refEnt2.equals(re2_2)) {
				re2Id2 = refEnt2.getId();
			} else {
				throw new IllegalStateException("unexpected instance");
			}
		}

		assertNotNull(re1Id1);
		assertNotNull(re1Id2);
		assertNotNull(re2Id1);
		assertNotNull(re2Id2);

		//

		tm.begin();

		try {
			newEntityManager();
			em = getEntityManager();
			em.joinTransaction();

			// mce = em.find(MultipleCollectionEntity.class, mceId1);
			// mce = em.merge(mce);

			assertEquals(2, mce.getRefEntities1().size());

			mce.removeRefEntity1(re1_2);
			assertEquals(1, mce.getRefEntities1().size());

			updatedRe1_1 = mce.getRefEntities1().get(0);
			assertEquals(re1_1, updatedRe1_1);
			updatedRe1_1.setText("MultipleCollectionRefEntity1-1-updated");

			re1_3 = new MultipleCollectionRefEntity1();
			re1_3.setText("MultipleCollectionRefEntity1-3");
			re1_3.setMultipleCollectionEntity(mce);
			mce.addRefEntity1(re1_3);
			assertEquals(2, mce.getRefEntities1().size());

			// -------------

			assertEquals(2, mce.getRefEntities2().size());

			mce.removeRefEntity2(re2_2);
			assertEquals(1, mce.getRefEntities2().size());

			updatedRe2_1 = mce.getRefEntities2().get(0);
			assertEquals(re2_1, updatedRe2_1);
			updatedRe2_1.setText("MultipleCollectionRefEntity2-1-updated");

			re2_3 = new MultipleCollectionRefEntity2();
			re2_3.setText("MultipleCollectionRefEntity2-3");
			re2_3.setMultipleCollectionEntity(mce);
			mce.addRefEntity2(re2_3);
			assertEquals(2, mce.getRefEntities2().size());

			mce = em.merge(mce);

		} finally {
			tryCommit(tm);
		}

		// re1Id3 = re1_3.getId();
		// re2Id3 = re2_3.getId();

		for (MultipleCollectionRefEntity1 adres : mce.getRefEntities1()) {
			if (adres.equals(re1_3)) {
				re1Id3 = adres.getId();
			}
		}

		for (MultipleCollectionRefEntity2 partner : mce.getRefEntities2()) {
			if (partner.equals(re2_3)) {
				re2Id3 = partner.getId();
			}
		}

		assertNotNull(re1Id3);
		assertNotNull(re2Id3);
	}

	@Test
	public void testRevisionsCounts() throws Exception {

		List<Number> mceId1Revs = getAuditReader().getRevisions(
				MultipleCollectionEntity.class, mceId1);
		List<Number> re1Id1Revs = getAuditReader().getRevisions(
				MultipleCollectionRefEntity1.class, re1Id1);
		List<Number> re1Id2Revs = getAuditReader().getRevisions(
				MultipleCollectionRefEntity1.class, re1Id2);
		List<Number> re1Id3Revs = getAuditReader().getRevisions(
				MultipleCollectionRefEntity1.class, re1Id3);
		List<Number> re2Id1Revs = getAuditReader().getRevisions(
				MultipleCollectionRefEntity2.class, re2Id1);
		List<Number> re2Id2Revs = getAuditReader().getRevisions(
				MultipleCollectionRefEntity2.class, re2Id2);
		List<Number> re2Id3Revs = getAuditReader().getRevisions(
				MultipleCollectionRefEntity2.class, re2Id3);

		assertEquals(Arrays.asList(1, 2, 3), mceId1Revs);
		assertEquals(Arrays.asList(2, 3), re1Id1Revs);
		assertEquals(Arrays.asList(2, 3), re1Id2Revs);
		assertEquals(Arrays.asList(3), re1Id3Revs);
		assertEquals(Arrays.asList(2, 3), re2Id1Revs);
		assertEquals(Arrays.asList(2, 3), re2Id2Revs);
		assertEquals(Arrays.asList(3), re2Id3Revs);

	}

	@Test
	public void testAuditJoinTable() throws Exception {
		List<AuditJoinTableInfo> mceRe1AuditJoinTableInfos = getAuditJoinTableRows(
				"MCE_RE1_AUD", "MCE_ID",
				"aud.originalId.MultipleCollectionEntity_id", "RE1_ID",
				"aud.originalId.refEntities1_id", "aud.originalId.REV",
				"aud.originalId.REV.id", "aud.REVTYPE");

		List<AuditJoinTableInfo> mceRe2AuditJoinTableInfos = getAuditJoinTableRows(
				"MCE_RE2_AUD", "MCE_ID",
				"aud.originalId.MultipleCollectionEntity_id", "RE2_ID",
				"aud.originalId.refEntities2_id", "aud.originalId.REV",
				"aud.originalId.REV.id", "aud.REVTYPE");

		assertEquals(4, mceRe1AuditJoinTableInfos.size());
		assertEquals(4, mceRe2AuditJoinTableInfos.size());

		DefaultRevisionEntity rev2 = new DefaultRevisionEntity();
		rev2.setId(2);
		DefaultRevisionEntity rev3 = new DefaultRevisionEntity();
		rev3.setId(3);

		assertEquals(new AuditJoinTableInfo("MCE_RE1_AUD", rev2,
				RevisionType.ADD, "MCE_ID", 1L, "RE1_ID", 1L),
				mceRe1AuditJoinTableInfos.get(0));
		assertEquals(new AuditJoinTableInfo("MCE_RE1_AUD", rev2,
				RevisionType.ADD, "MCE_ID", 1L, "RE1_ID", 2L),
				mceRe1AuditJoinTableInfos.get(1));
		assertEquals(new AuditJoinTableInfo("MCE_RE1_AUD", rev3,
				RevisionType.DEL, "MCE_ID", 1L, "RE1_ID", 2L),
				mceRe1AuditJoinTableInfos.get(2));
		assertEquals(new AuditJoinTableInfo("MCE_RE1_AUD", rev3,
				RevisionType.ADD, "MCE_ID", 1L, "RE1_ID", 3L),
				mceRe1AuditJoinTableInfos.get(3));

		assertEquals(new AuditJoinTableInfo("MCE_RE2_AUD", rev2,
				RevisionType.ADD, "MCE_ID", 1L, "RE2_ID", 1L),
				mceRe2AuditJoinTableInfos.get(0));
		assertEquals(new AuditJoinTableInfo("MCE_RE2_AUD", rev2,
				RevisionType.ADD, "MCE_ID", 1L, "RE2_ID", 2L),
				mceRe2AuditJoinTableInfos.get(1));
		assertEquals(new AuditJoinTableInfo("MCE_RE2_AUD", rev3,
				RevisionType.DEL, "MCE_ID", 1L, "RE2_ID", 2L),
				mceRe2AuditJoinTableInfos.get(2));
		assertEquals(new AuditJoinTableInfo("MCE_RE2_AUD", rev3,
				RevisionType.ADD, "MCE_ID", 1L, "RE2_ID", 3L),
				mceRe2AuditJoinTableInfos.get(3));

	}

	private List<AuditJoinTableInfo> getAuditJoinTableRows(
			String middleEntityName, String joinColumnIdName,
			String joinColumnIdProp, String inverseJoinColumnIdName,
			String inverseJoinColumnIdProp, String revProp, String revIdProp,
			String revTypeProp) throws Exception {

		StringBuilder qryBuilder = new StringBuilder("select ");
		qryBuilder.append("aud ");
		qryBuilder.append(", ").append(joinColumnIdProp)
				.append(" as joinColumnId");
		qryBuilder.append(", ").append(inverseJoinColumnIdProp)
				.append(" as inverseJoinColumnId");
		qryBuilder.append(", ").append(revProp).append(" as rev");
		qryBuilder.append(", ").append(revIdProp).append(" as revId");
		qryBuilder.append(", ").append(revTypeProp).append(" as revType");
		qryBuilder.append(" from ").append(middleEntityName).append(" aud ");
		qryBuilder
				.append(" order by joinColumnId asc, inverseJoinColumnId asc, revId asc");

		String query = qryBuilder.toString();

		newEntityManager();
		EntityManager em = getEntityManager();
		Query qry = em.createQuery(query);

		@SuppressWarnings("unchecked")
		List<Object[]> auditJoinTableRows = qry.getResultList();
		List<AuditJoinTableInfo> result = new ArrayList<AuditJoinTableInfo>(
				auditJoinTableRows.size());

		for (Object[] auditJoinTableRow : auditJoinTableRows) {
			Long joinColumnId = (Long) auditJoinTableRow[1];
			Long inverseJoinColumnId = (Long) auditJoinTableRow[2];
			DefaultRevisionEntity rev = (DefaultRevisionEntity) auditJoinTableRow[3];
			RevisionType revType = (RevisionType) auditJoinTableRow[5];

			AuditJoinTableInfo info = new AuditJoinTableInfo(middleEntityName,
					rev, revType, joinColumnIdName, joinColumnId,
					inverseJoinColumnIdName, inverseJoinColumnId);
			result.add(info);

			LOG.error("Found: " + info);

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

		private AuditJoinTableInfo(String name, DefaultRevisionEntity rev,
				RevisionType revType, String joinColumnName, Long joinColumnId,
				String inverseJoinColumnName, Long inverseJoinColumnId) {
			super();
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
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((inverseJoinColumnId == null) ? 0 : inverseJoinColumnId
							.hashCode());
			result = prime * result
					+ ((joinColumnId == null) ? 0 : joinColumnId.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((revId == null) ? 0 : revId.hashCode());
			result = prime * result
					+ ((revType == null) ? 0 : revType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AuditJoinTableInfo other = (AuditJoinTableInfo) obj;
			if (inverseJoinColumnId == null) {
				if (other.inverseJoinColumnId != null)
					return false;
			} else if (!inverseJoinColumnId.equals(other.inverseJoinColumnId))
				return false;
			if (joinColumnId == null) {
				if (other.joinColumnId != null)
					return false;
			} else if (!joinColumnId.equals(other.joinColumnId))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (revId == null) {
				if (other.revId != null)
					return false;
			} else if (!revId.equals(other.revId))
				return false;
			if (revType != other.revType)
				return false;
			return true;
		}

	}

}
