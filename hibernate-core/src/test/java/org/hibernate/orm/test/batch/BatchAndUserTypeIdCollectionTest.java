/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;
import org.hibernate.HibernateException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.usertype.EnhancedUserType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.type.SqlTypes.VARCHAR;

@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "false" )
		}
)
@DomainModel(
		annotatedClasses = {
				BatchAndUserTypeIdCollectionTest.Child.class,
				BatchAndUserTypeIdCollectionTest.Parent.class
		}
)
@SessionFactory(
		useCollectingStatementInspector = true
)

public class BatchAndUserTypeIdCollectionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for (long i = 1L; i < 11; i++) {
						Parent parent = new Parent( new Parent.ParentId( "parent-" + i ) );
						Child child1 = new Child( i * 100L + 1L, parent );
						Child child2 = new Child( i * 100L + 2L, parent );
						Child child3 = new Child( i * 100L + 3L, parent );
						Child child4 = new Child( i * 100L + 4L, parent );
						Child child5 = new Child( i * 100L + 5L, parent );
						Child child6 = new Child( i * 100L + 6L, parent );
						Child child7 = new Child( i * 100L + 7L, parent );
						Child child8 = new Child( i * 100L + 8L, parent );
						Child child9 = new Child( i * 100L + 9L, parent );
						Child child10 = new Child( i * 100L + 10L, parent );
						Child child11 = new Child( i * 100L + 11L, parent );
						session.persist( parent );
					}
				}
		);
	}

	@Test
	public void testBatchInitializeChildCollection(SessionFactoryScope scope){
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					final List<Parent> list = session.createSelectionQuery( "from Parent", Parent.class )
							.getResultList();
					list.get( 0 ).getChildren().size();
					statementInspector.assertExecutedCount( 2 );
					Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( "?" );
					if ( scope.getSessionFactory().getJdbcServices().getDialect().useArrayForMultiValuedParameters() ) {
						Assertions.assertThat( statementInspector.getSqlQueries().get( 1 ) ).containsOnlyOnce( "?" );
					}
					else {
						Assertions.assertThat( statementInspector.getSqlQueries().get( 1 ) ).containsOnlyOnce( "in (?,?,?,?,?)" );
					}
				}
		);
	}

	@Entity(name = "Child")
	@Table(name = "child_table")
	public static class Child {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		private Parent parent;

		public Child() {
		}

		public Child(Long id, Parent parent) {
			this.id = id;
			this.name = String.valueOf( id );
			this.parent = parent;
			parent.addChild( this );
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Parent getParent() {
			return parent;
		}

	}

	@Entity(name = "Parent")
	@Table(name = "parents")
	public static class Parent {
		@Id
		@Type(ParentIdUserType.class)
		private ParentId id;

		private String name;

		@BatchSize(size = 5)
		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
		public Set<Child> children = new HashSet<>();

		public Parent() {
		}

		public Parent(ParentId id) {
			this.id = id;
			this.name = String.valueOf( id );
		}

		public ParentId getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void addChild(Child child){
			children.add( child );
		}

		public static class ParentId implements Serializable {

			private static final long serialVersionUID = 1L;

			private final String id;

			@Override
			public String toString() {
				return id;
			}

			public ParentId(String id) {
				this.id = id;
			}

			public String getId() {
				return id;
			}

			@Override
			public boolean equals(Object o) {
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				ParentId parentId = (ParentId) o;
				return Objects.equals( id, parentId.id );
			}

			@Override
			public int hashCode() {
				return Objects.hashCode( id );
			}
		}


		static class ParentIdUserType implements EnhancedUserType<ParentId> {
			@Override
			public int getSqlType() {
				return VARCHAR;
			}

			@Override
			public Class<ParentId> returnedClass() {
				return ParentId.class;
			}

			@Override
			public boolean equals(ParentId x, ParentId y) {
				return Objects.equals(x, y);
			}

			@Override
			public int hashCode(ParentId x) {
				return x.hashCode();
			}

			@Override
			public ParentId nullSafeGet(ResultSet rs, int position,
										SharedSessionContractImplementor session, Object owner)
					throws SQLException {
				String string = rs.getString( position );
				return rs.wasNull() ? null : new ParentId(string);
			}

			@Override
			public void nullSafeSet(PreparedStatement st, ParentId value, int index,
									SharedSessionContractImplementor session)
					throws SQLException {
				if ( value == null ) {
					st.setNull(index, VARCHAR);
				}
				else {
					st.setString(index, value.getId());
				}
			}

			@Override
			public boolean isMutable() {
				return false;
			}

			@Override
			public ParentId deepCopy(ParentId parentId) {
				return parentId; //ParentId is immutable
			}

			@Override
			public Serializable disassemble(ParentId parentId) {
				return parentId; //ParentId is immutable
			}

			@Override
			public ParentId assemble(Serializable cached, Object owner) {
				return (ParentId) cached; //ParentId is immutable
			}

			@Override
			public String toSqlLiteral(ParentId parentId) {
				return parentId.getId();
			}

			@Override
			public String toString(ParentId parentId) throws HibernateException {
				return parentId.getId();
			}

			@Override
			public ParentId fromStringValue(CharSequence sequence) throws HibernateException {
				return new ParentId(sequence.toString());
			}

			@Override
			public AttributeConverter<ParentId, ?> getValueConverter() {
				return new AttributeConverter<ParentId, String>() {
					@Override
					public String convertToDatabaseColumn(ParentId value) {
						return value == null ? null : value.getId();
					}

					@Override
					public ParentId convertToEntityAttribute(String dbData) {
						return dbData == null ? null : new ParentId(dbData);
					}
				};
			}
		}
	}
}
