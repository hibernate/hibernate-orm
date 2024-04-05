/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.property;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
			FieldMappingWithGetterAndIsTest2.Tester.class
		}
)
@SessionFactory
@BytecodeEnhanced(testEnhancedClasses = FieldMappingWithGetterAndIsTest2.Tester.class)
@EnhancementOptions( inlineDirtyChecking = true, lazyLoading = true )
public class FieldMappingWithGetterAndIsTest2 {

	@Test
	public void testResolution(SessionFactoryScope scope) {
		scope.getSessionFactory();
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
