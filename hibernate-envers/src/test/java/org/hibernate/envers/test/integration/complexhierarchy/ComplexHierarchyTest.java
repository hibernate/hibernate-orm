package org.hibernate.envers.test.integration.complexhierarchy;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-10212")
public class ComplexHierarchyTest extends BaseEnversJPAFunctionalTestCase {

	private static final Logger log = Logger.getLogger( ComplexHierarchyTest.class );
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			ComplexParentEntity.class,
			ChildEntity.class,
			ChildDiscrepancy.class
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void addConfigOptions(Map options) {
		options.put("org.hibernate.envers.track_entities_changed_in_revision", "true");
		options.put("org.hibernate.envers.storeDataAtDelete", "true");
		options.put("org.hibernate.envers.modified_flag_suffix", "_M");
	}
	
	@Test
	@Priority(10)
	public void initData() {
		
		EntityManager em = getEntityManager();

		final ComplexParentEntity parent = new ComplexParentEntity();
		parent.setId(1L);
		parent.setIssueDate(new Date());
		
		final ChildEntity child = new ChildEntity();
		child.setId(1L);
		child.setProperty("value1");
		parent.setChild(child);

		em.getTransaction().begin();
		em.persist(child);
		em.persist(parent);
		em.getTransaction().commit();

		em.getTransaction().begin();
		ChildEntity child2 = parent.getChild();
		child2.setProperty("value2");
		child2 = em.merge(child2);
		parent.setChild(child2);
		em.getTransaction().commit();
		
	}

	@Test
	public void testShouldCreateOnlyOneRevision() {
				
		List<Object[]> revisionsAfterUpdate = getRevisions();
		Assert.assertTrue(revisionsAfterUpdate.size() == 1);
				
	}
	
	@SuppressWarnings("unchecked")
	private List<Object[]> getRevisions() {		
		List<Object[]> revisions = getAuditReader()
					.createQuery()
					.forRevisionsOfEntity(ComplexParentEntity.class, false, false)
					.getResultList();
		log.infov("There are {0} revisions.", revisions.size());
		return revisions;
	}
	
}
