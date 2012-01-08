package org.hibernate.test.inheritancediscriminator;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class PersistChildEntitiesWithDiscriminatorTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ParentEntity.class, InheritingEntity.class };
	}

	@Test
	public void shouldPersistTwoEntities() {
		Session session = openSession();
		session.beginTransaction();
		InheritingEntity child1 = new InheritingEntity();
		InheritingEntity child2 = new InheritingEntity();
		child1.setSomeValue("blabla");
		session.save(child1);

		session.flush();
		session.clear();
		InheritingEntity loaded = (InheritingEntity) session.load(InheritingEntity.class, child1.getId());
		loaded.getId();

		session.save(child2);
		session.getTransaction().rollback();
	}

}
