package org.hibernate.test.hql;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;

public class EntityBeanTest extends TestCase {


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{EntityBean.class,Item.class};
	}
	public void testAAA(){
		Session s=openSession();
		s.beginTransaction();
		Item item = new Item();
		item.setItemValue("1");
		EntityBean eb = new EntityBean();
		eb.getItems().add(item);
		item=new Item();
		item.setItemValue("2");
		eb.getItems().add(item);
		s.persist(eb);
		s.getTransaction().commit();
		s.close();
		
		s=openSession();
		s.beginTransaction();
		Query query = s.createQuery("SELECT o FROM EntityBean AS o, IN (o.items)  l WHERE l.itemValue = '1'");
		List list = query.list();
		assertEquals(list.size(), 1);
		s.getTransaction().commit();
		s.close();
	}

}
