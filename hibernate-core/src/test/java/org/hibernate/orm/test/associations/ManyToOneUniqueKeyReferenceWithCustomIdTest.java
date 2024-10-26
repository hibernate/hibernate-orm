/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.associations;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.EnhancedUserType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Kowsar Atazadeh
 */
@SessionFactory
@DomainModel(annotatedClasses =
		{
				ManyToOneUniqueKeyReferenceWithCustomIdTest.Phone.class,
				ManyToOneUniqueKeyReferenceWithCustomIdTest.User.class
		})
@JiraKey("HHH-18764")
public class ManyToOneUniqueKeyReferenceWithCustomIdTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Phone phone = new Phone();

			User user1 = new User( new CustomId( "u1" ), "Kowsar" );
			session.persist( user1 );
			phone.setUser( user1 );
			session.persist( phone );

			User user2 = new User( new CustomId( "u2" ), "Someone" );
			session.persist( user2 );
			phone.setUser( user2 );
			session.persist( phone );
		} );
	}

	@Entity(name = "Phone")
	static class Phone {
		@Id
		@GeneratedValue
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(referencedColumnName = "name", nullable = false)
		User user;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}
	}

	@Entity(name = "_User")
	static class User {
		@Id
		@Type(CustomIdType.class)
		CustomId id;

		@NaturalId
		String name;

		public User() {
		}

		public User(CustomId id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public CustomId getId() {
			return id;
		}

		public void setId(CustomId id) {
			this.id = id;
		}
	}

	static class CustomIdType implements EnhancedUserType<CustomId> {
		@Override
		public String toSqlLiteral(CustomId value) {
			return "'" + value.toString() + "'";
		}

		@Override
		public String toString(CustomId value) throws HibernateException {
			return value.toString();
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st, CustomId value, int position,
				SharedSessionContractImplementor session) throws SQLException {
			st.setObject( position, value.toString(), getSqlType() );
		}

		@Override
		public CustomId nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
				throws SQLException {
			String idValue = rs.getString( position );
			return idValue != null ? fromStringValue( idValue ) : null;
		}

		@Override
		public CustomId fromStringValue(CharSequence sequence) throws HibernateException {
			return new CustomId( sequence.toString() );
		}

		@Override
		public int getSqlType() {
			return Types.VARCHAR;
		}

		@Override
		public Class<CustomId> returnedClass() {
			return CustomId.class;
		}

		@Override
		public CustomId deepCopy(CustomId value) {
			return new CustomId( value.getId() );
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(CustomId value) {
			return value;
		}

		@Override
		public CustomId assemble(Serializable cached, Object owner) {
			return (CustomId) cached;
		}

		@Override
		public boolean equals(CustomId x, CustomId y) {
			return Objects.equals( x, y );
		}

		@Override
		public int hashCode(CustomId x) {
			return x != null ? x.hashCode() : 0;
		}
	}

	static class CustomId implements Serializable {
		private String id;

		public CustomId() {
		}

		public CustomId(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
