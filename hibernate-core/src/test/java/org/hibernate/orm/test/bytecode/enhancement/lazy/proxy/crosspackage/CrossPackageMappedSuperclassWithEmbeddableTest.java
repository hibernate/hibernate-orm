/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.base.EmbeddableType;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.derived.TestEntity;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				TestEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class CrossPackageMappedSuperclassWithEmbeddableTest {

	@Test
	@JiraKey("HHH-15141")
	public void testIt(SessionFactoryScope scope) {
		// Just a smoke test; the original failure happened during bytecode enhancement.
		Long id = scope.fromTransaction( s -> {
			TestEntity testEntity = new TestEntity();
			EmbeddableType embedded = new EmbeddableType();
			embedded.setField( "someValue" );
			testEntity.setEmbeddedField( embedded );
			s.persist( testEntity );
			return testEntity.getId();
		} );
		scope.inTransaction( s -> {
			TestEntity testEntity = s.find( TestEntity.class, id );
			assertThat( testEntity.getEmbeddedField().getField() ).isEqualTo( "someValue" );
		} );
	}

}
