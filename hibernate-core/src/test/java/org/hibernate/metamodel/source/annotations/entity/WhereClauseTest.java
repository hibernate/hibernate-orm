package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.Where;
import org.hibernate.metamodel.binding.EntityBinding;

import static junit.framework.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */
public class WhereClauseTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = Foo.class)
	public void testWhereFilter() {
		EntityBinding binding = getEntityBinding( Foo.class );
		assertEquals( "Wrong where filter", "1=1", binding.getWhereFilter() );
	}

	@Entity
	@Where(clause = "1=1")
	class Foo {
		@Id
		private long id;
	}
}


