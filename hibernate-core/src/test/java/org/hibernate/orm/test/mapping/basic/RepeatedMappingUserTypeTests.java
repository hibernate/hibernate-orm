/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-16250")
@DomainModel(annotatedClasses = RepeatedMappingUserTypeTests.TheEntity.class)
@SessionFactory
public class RepeatedMappingUserTypeTests {

	@Test
	public void testResolution(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new TheEntity( 1, 123, 456, 789 ) );
					session.flush();
					session.clear();
					final TheEntity theEntity = session.find( TheEntity.class, 1 );
					assertNotNull( theEntity.getSortedIds() );
					assertNotNull( theEntity.getSortedIdsExpression() );
					assertEquals( new TreeSet<>( Arrays.asList( 123, 456, 789 ) ), theEntity.getSortedIds() );
				}
		);
	}

	@Entity(name = "TheEntity")
	public static class TheEntity {
		@Id
		private Integer id;

		@Type(CodeJavaType.class)
		@Column(name = "SORTED_IDS", nullable = false)
		private SortedSet<Integer> sortedIds = new TreeSet<>();

		@Column(name = "SORTED_IDS", nullable = false, insertable = false, updatable = false)
		private String sortedIdsExpression;

		public TheEntity() {
		}

		public TheEntity(Integer id, Integer... ids) {
			this.id = id;
			this.sortedIds.addAll( Arrays.asList( ids ) );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SortedSet<Integer> getSortedIds() {
			return sortedIds;
		}

		public void setSortedIds(SortedSet<Integer> sortedIds) {
			this.sortedIds = sortedIds;
		}

		public String getSortedIdsExpression() {
			return sortedIdsExpression;
		}

		public void setSortedIdsExpression(String sortedIdsExpression) {
			this.sortedIdsExpression = sortedIdsExpression;
		}
	}

	public static class CodeJavaType implements UserType<SortedSet<Integer>>,
			AttributeConverter<SortedSet<Integer>, String> {

		@Override
		public int getSqlType() {
			return SqlTypes.VARCHAR;
		}

		@Override
		public Class<SortedSet<Integer>> returnedClass() {
			//noinspection unchecked
			return (Class) Set.class;
		}

		@Override
		public boolean equals(SortedSet<Integer> x, SortedSet<Integer> y) {
			return Objects.equals( x, y );
		}

		@Override
		public int hashCode(SortedSet<Integer> x) {
			return Objects.hashCode( x );
		}

		@Override
		public SortedSet<Integer> nullSafeGet(
				ResultSet rs,
				int position,
				WrapperOptions options) throws SQLException {
			return convertToEntityAttribute( rs.getString( position ) );
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st,
				SortedSet<Integer> values,
				int index,
				WrapperOptions options) throws SQLException {
			if ( values == null || values.isEmpty() ) {
				st.setNull( index, SqlTypes.VARCHAR );
				return;
			}

			String databaseValue = convertToDatabaseColumn( values );

			st.setString( index, databaseValue );
		}

		@Override
		public SortedSet<Integer> deepCopy(SortedSet<Integer> value) {
			return new TreeSet<>( value );
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public Serializable disassemble(SortedSet<Integer> value) {
			return (Serializable) value;
		}

		@Override
		public SortedSet<Integer> assemble(Serializable cached, Object owner) {
			return (SortedSet<Integer>) cached;
		}

		@Override
		public String convertToDatabaseColumn(SortedSet<Integer> values) {
			return values.stream().map( Object::toString ).collect( Collectors.joining( "|", "|", "|" ) );
		}

		@Override
		public SortedSet<Integer> convertToEntityAttribute(String databaseValue) {
			return Arrays.stream( databaseValue.split( "\\|" ) )
					.map( value -> value.trim() )
					.filter( value -> !value.isEmpty() )
					.map( value -> Integer.valueOf( value ) )
					.collect( Collectors.toCollection( TreeSet::new ) );
		}

	}
}
