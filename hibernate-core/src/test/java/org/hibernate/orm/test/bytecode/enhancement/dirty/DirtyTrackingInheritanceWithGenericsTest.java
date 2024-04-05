/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Yoann Rodière
 * @author Marco Belladelli
 */
@BytecodeEnhanced
@EnhancementOptions( inlineDirtyChecking = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16459" )
public class DirtyTrackingInheritanceWithGenericsTest {
	@Test
	public void testOne() {
		final ChildItemOne entity = new ChildItemOne();
		EnhancerTestUtils.checkDirtyTracking( entity );
		entity.setBasicValue( "basic_value" );
		entity.setAssociation( new OtherOne() );
		EnhancerTestUtils.checkDirtyTracking( entity, "basicValue", "association" );
	}

	@Test
	public void testTwo() {
		final ChildItemTwo entity = new ChildItemTwo();
		EnhancerTestUtils.checkDirtyTracking( entity );
		entity.setBasicValue( 123 );
		entity.setAssociation( new OtherTwo() );
		EnhancerTestUtils.checkDirtyTracking( entity, "basicValue", "association" );
	}

	@Entity( name = "OtherOne" )
	public static class OtherOne {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity( name = "OtherTwo" )
	public static class OtherTwo {
		@Id
		@GeneratedValue
		private Long id;
	}

	@MappedSuperclass
	public static abstract class Item<T, S> {
		private T basicValue;
		@ManyToOne
		private S association;

		public T getBasicValue() {
			return basicValue;
		}

		public void setBasicValue(T basicValue) {
			this.basicValue = basicValue;
		}

		public S getAssociation() {
			return association;
		}

		public void setAssociation(S association) {
			this.association = association;
		}
	}

	@Entity( name = "ChildItemOne" )
	public static class ChildItemOne extends Item<String, OtherOne> {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "ChildItemTwo" )
	public static class ChildItemTwo extends Item<Integer, OtherTwo> {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
