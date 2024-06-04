package org.hibernate.orm.test.query.hhh18218;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@SessionFactory
@DomainModel(annotatedClasses = {
		HHH18218Test.MyConcreteEntity.class,
		HHH18218Test.AbstractEntity.class
})
@JiraKey("HHH-18218")
public class HHH18218Test {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "select new org.hibernate.orm.test.query.hhh18218.MyConcreteDto(e.id, e.someOtherField) from MyConcreteEntity e", MyConcreteDto.class )
					.getResultList();
		} );
	}


	@MappedSuperclass
	abstract static class AbstractEntity<K extends Serializable> /*extends PanacheEntityBase*/ {
		@Id
		protected K id;

		protected K getId() {
			return id;
		}

		protected void setId(K id) {
			this.id = id;
		}
	}

	@Entity(name = "MyConcreteEntity")
	static class MyConcreteEntity extends AbstractEntity<Long> {
		protected String someOtherField;

		public String getSomeOtherField() {
			return someOtherField;
		}
	}

}
