/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.jta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefIngEntity;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 * @author VladoKuruc
 */
@TestForIssue( jiraKey = "HHH-14061")
public class OneToManyLazyJtaSessionClosedBeforeCommitTest extends BaseEnversJPAFunctionalTestCase {
	private Integer parentId;
	private Integer entityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {SetRefIngEntity.class, SetRefEdEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
	}

	@Test
	@Priority(10)
	public void initData() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = getEntityManager();
		try {
			SetRefIngEntity refIngEntity = new SetRefIngEntity( 3, "ingEntityRef" );
			entityManager.persist( refIngEntity );

			SetRefEdEntity edEntity = new SetRefEdEntity( 2, "edEntity" );
			edEntity.setRef(refIngEntity);			
			entityManager.persist( edEntity );
			parentId = edEntity.getId();
			
			SetRefIngEntity ingEntity = new SetRefIngEntity( 1, "ingEntity" );

			Set<SetRefIngEntity> sries = new HashSet<>();
			sries.add( ingEntity );
			ingEntity.setReference( edEntity );
			edEntity.setReffering( sries );

			entityManager.persist( ingEntity );

			entityId = ingEntity.getId();
		}
		finally {
			entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		entityManager = getEntityManager();
		try {
            entityManager.unwrap(Session.class).setHibernateFlushMode(FlushMode.MANUAL);
            SetRefEdEntity edEntity = entityManager.find(SetRefEdEntity.class, parentId);
            Set<SetRefIngEntity> reffering = edEntity.getReffering();
            SetRefIngEntity ingEntity = reffering.iterator().next();
            ingEntity.setReference(null);
            reffering.remove(ingEntity);
            entityManager.merge(ingEntity);
            entityManager.flush();
            //clear context in transaction
            entityManager.clear();
            entityManager.merge(edEntity);
            entityManager.flush();
		}
		finally {
         	entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals(
				Arrays.asList(1, 2),
				getAuditReader().getRevisions( SetRefIngEntity.class, entityId )
		);
		assertEquals(
				Arrays.asList(1, 2),
				getAuditReader().getRevisions( SetRefEdEntity.class, parentId )
		);
	}

	@Test
	public void testRevisionHistory() {
        assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( SetRefIngEntity.class, entityId ) );
        assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( SetRefEdEntity.class, parentId ) );
	}
}
