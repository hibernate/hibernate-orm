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
		annotatedClasses = ContributedUserTypeTest.StringWrapperTestEntity.class,
		typeContributors = StringWrapperTypeContributor.class
)
@SessionFactory
public class ContributedUserTypeTest {


	@Test
	@JiraKey( "HHH-14408" )
	public void test(SessionFactoryScope scope) {
		Type type = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( StringWrapperTestEntity.class )
				.getPropertyType( "stringWrapper" );
		Assertions.assertTrue( type instanceof CustomType, "Type was initialized too early i.e. before type-contributors were run" );
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

	@Entity(name = "StringWrapperTestEntity")
	public static class StringWrapperTestEntity implements Serializable {
		@Id
		private Integer id;
		private StringWrapper stringWrapper;
	}

}
