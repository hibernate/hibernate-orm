/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.id.uuid.annotation;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {TheEntity.class, TheOtherEntity.class} )
@SessionFactory
@SkipForDialect( dialectClass = SybaseDialect.class, matchSubTypes = true,
		reason = "Skipped for Sybase to avoid problems with UUIDs potentially ending with a trailing 0 byte")
public class UuidGeneratorAnnotationTests {
	@Test
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( TheEntity.class, (descriptor) -> {
			final Property idProperty = descriptor.getIdentifierProperty();
			final BasicValue value = (BasicValue) idProperty.getValue();

			assertThat( value.getCustomIdGeneratorCreator() ).isNotNull();

//			final String strategy = value.getIdentifierGeneratorStrategy();
//			assertThat( strategy ).isEqualTo( "assigned" );
		} );
	}

	@Test
	public void basicUseTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			TheEntity steve = new TheEntity("steve");
			session.persist( steve );
			session.flush();
			assertThat( steve.id ).isNotNull();
		} );
	}

	@Test
	public void nonPkUseTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			TheOtherEntity gavin = new TheOtherEntity("gavin");
			session.persist( gavin );
			session.flush();
			assertThat( gavin.id ).isNotNull();
		} );
	}
}
