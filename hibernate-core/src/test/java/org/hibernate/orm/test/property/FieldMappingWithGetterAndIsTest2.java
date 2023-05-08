/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.property;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions( inlineDirtyChecking = true, lazyLoading = true )
public class FieldMappingWithGetterAndIsTest2 extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Tester.class );
	}

	@Test
	public void testResolution() {
		sessionFactory();
	}

	@Entity(name="Tester")
	@Table(name="Tester")
	public static class Tester {
		@Id
		private Integer id;
		@Basic
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public boolean isName() {
			return name != null;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
