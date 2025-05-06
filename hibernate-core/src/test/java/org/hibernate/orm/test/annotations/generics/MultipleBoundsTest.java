/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DomainModel(
		annotatedClasses = {
				MultipleBoundsTest.User.class,
				MultipleBoundsTest.UserTranslation.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-15687")
public class MultipleBoundsTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User user = new User( 1l, "First User" );
					session.persist( user );

					UserTranslation translation = new UserTranslation( 1l, "T", user );
					session.persist( translation );
				}
		);
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<AbstractEntity> resultList = session.createQuery( "from UserTranslation where classifier.id=?1" )
							.setParameter( 1, 1l )
							.getResultList();
					assertThat( resultList.size() ).isEqualTo( 1 );
					AbstractEntity user = resultList.get( 0 );
					assertInstanceOf( UserTranslation.class, user );
				}
		);
	}

	@MappedSuperclass
	public static abstract class AbstractEntity {

		@Id
		private Long id;

		public AbstractEntity() {
		}

		public AbstractEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	public interface WithTranslationKey {
		String getTranslationKey();
	}

	@MappedSuperclass
	public static abstract class AbstractTranslationEntity<T extends AbstractEntity & WithTranslationKey>
			extends AbstractEntity {

		public AbstractTranslationEntity() {
		}

		public AbstractTranslationEntity(Long id, T classifier) {
			super( id );
			this.classifier = classifier;
		}

		@JoinColumn(name = "CLASSIFIER_ID", nullable = false)
		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private T classifier;

		public T getClassifier() {
			return classifier;
		}

		public void setClassifier(T classifier) {
			this.classifier = classifier;
		}

	}

	@Entity(name = "User")
	@Table(name = "USERS")
	public static class User extends AbstractEntity implements WithTranslationKey {

		private String name;

		public User() {
		}

		public User(Long id, String name) {
			setId( id );
			this.name = name;
		}

		@Override
		public String getTranslationKey() {
			return name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "UserTranslation")
	public static class UserTranslation extends AbstractTranslationEntity<User> {

		@Column(name = "TRANSLATION_COLUMN")
		private String translation;

		public UserTranslation() {
		}

		public UserTranslation(Long id, String translation, User user) {
			super( id, user );
			this.translation = translation;
		}

		public String getTranslation() {
			return translation;
		}

		public void setTranslation(String translation) {
			this.translation = translation;
		}
	}

}
