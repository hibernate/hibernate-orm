package org.hibernate.ejb.test.mapping;

import junit.framework.Assert;
import org.hibernate.ejb.test.TestCase;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

public class HybridBindingTest extends TestCase {

	private HybridDao dao = new HybridDaoImpl();

	public HybridBindingTest(String name) {
		super(name);
	}

	@Override
	public String[] getMappings() {
		return new String[]{"org/hibernate/ejb/test/mapping/HybridHbm.hbm.xml"};
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{HybridAnnot.class};
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		EntityManager em = getOrCreateEntityManager();
		dao = new HybridDaoImpl();
		dao.setEntityManager(em);

		em.getTransaction().begin();
		for (int i = 0; i < 3; i++) {
			HybridHbm entity = new HybridHbm();
			entity.setId(String.valueOf(i));
			entity.setText("text");
			em.persist(entity);
		}
		em.getTransaction().commit();
	}

	public void testAnnotQl() {
		List<HybridAnnot> results = dao.getAnnotQl();

		Assert.assertNotNull(results);
		Assert.assertEquals(3, results.size());
	}

	public void testAnnotCriteria() {
		List<HybridAnnot> results = dao.getAnnotCriteria();

		Assert.assertNotNull(results);
		Assert.assertEquals(3, results.size());
	}

	public void testHbmQl() {
		List<HybridHbm> results = dao.getHbmQl();

		Assert.assertNotNull(results);
		Assert.assertEquals(3, results.size());
	}

	public void testHbmCriteria() {
		List<HybridHbm> results = dao.getHbmCriteria();

		Assert.assertNotNull(results);
		Assert.assertEquals(3, results.size());
	}

	interface HybridDao {
		List<HybridAnnot> getAnnotQl();
		List<HybridAnnot> getAnnotCriteria();
		List<HybridHbm> getHbmQl();
		List<HybridHbm> getHbmCriteria();

		void setEntityManager(EntityManager orCreateEntityManager);
	}

	class HybridDaoImpl implements HybridDao {

		private EntityManager entityManager;

		public List<HybridAnnot> getAnnotQl() {
			return getQl("from HybridAnnot");
		}

		public List<HybridAnnot> getAnnotCriteria() {
			return getCriteria(HybridAnnot.class);
		}

		public List<HybridHbm> getHbmQl() {
			return getQl("from HybridHbm");
		}

		public List<HybridHbm> getHbmCriteria() {
			return getCriteria(HybridHbm.class);
		}

		public void setEntityManager(EntityManager entityManager) {
			this.entityManager = entityManager;
		}

		private <T> List<T> getQl(String qlString) {
			return entityManager.createQuery(qlString).getResultList();
		}

		private <T> List<T> getCriteria(Class<T> entityClass) {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<T> cq = cb.createQuery(entityClass);
			cq.from(entityClass);
			return entityManager.createQuery(cq).getResultList();
		}

	}
}
