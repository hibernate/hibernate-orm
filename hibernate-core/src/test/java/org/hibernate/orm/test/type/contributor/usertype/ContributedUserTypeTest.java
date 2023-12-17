/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type.contributor.usertype;

import java.io.Serializable;

import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernate.type.UserComponentType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				ContributedUserTypeTest.StringWrapperTestEntity.class,
				ContributedUserTypeTest.MyCompositeValueTestEntity.class,
		},
		typeContributors = { StringWrapperTypeContributor.class, MyCompositeValueTypeContributor.class }
)
@SessionFactory
public class ContributedUserTypeTest {
	@Test
	@JiraKey( "HHH-14408" )
	public void test(SessionFactoryScope scope) {
		final Type type = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( StringWrapperTestEntity.class )
				.getPropertyType( "stringWrapper" );
		Assertions.assertTrue(
				type instanceof CustomType,
				"Type was initialized too early i.e. before type-contributors were run"
		);
	}

	@Test
	@JiraKey( "HHH-17181" )
	public void testComposite(SessionFactoryScope scope) {
		final Type type = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( MyCompositeValueTestEntity.class )
				.getPropertyType( "compositeValue" );
		Assertions.assertInstanceOf( UserComponentType.class, type );
	}

	@Test
	@JiraKey( "HHH-17100" )
	public void testParameter(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.createSelectionQuery( "from StringWrapperTestEntity e where e.stringWrapper = :p" )
							.setParameter( "p", new StringWrapper( "abc" ) )
							.getResultList();
				}
		);
	}

	@Test
	@JiraKey( "HHH-17181" )
	public void testCompositeParameter(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.createSelectionQuery( "from MyCompositeValueTestEntity e where e.compositeValue = :c" )
							.setParameter( "c", new MyCompositeValue( 1L, "1" ) )
							.getResultList();
				}
		);
	}

	@Entity( name = "StringWrapperTestEntity" )
	public static class StringWrapperTestEntity implements Serializable {
		@Id
		private Integer id;
		private StringWrapper stringWrapper;
	}

	@Entity( name = "MyCompositeValueTestEntity" )
	public static class MyCompositeValueTestEntity implements Serializable {
		@Id
		private Integer id;
		private MyCompositeValue compositeValue;
	}
}
