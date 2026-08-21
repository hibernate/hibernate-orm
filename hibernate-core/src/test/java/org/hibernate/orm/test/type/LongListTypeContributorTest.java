/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		typeContributors = LongListTypeContributorTest.StringifiedCollectionTypeContributor.class,
		annotatedClasses = LongListTypeContributorTest.SpecialItem.class
)
@SessionFactory
public class LongListTypeContributorTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey(value = "HHH-11409")
	public void testParameterRegisteredCollection(SessionFactoryScope factoryScope) {
		LongList longList = new LongList( 5L, 11L, 6123L, -61235L, 24L );

		factoryScope.inTransaction( em -> {
			SpecialItem item = new SpecialItem( "LongList", longList );
			em.persist( item );
		} );

		factoryScope.inTransaction( em -> {
			SpecialItem item = em.createNativeQuery(
							"SELECT * FROM special_table WHERE long_list = ?", SpecialItem.class )
					.setParameter( 1, longList )
					.getSingleResult();

			assertEquals( "LongList", item.getName() );
		} );

		factoryScope.inTransaction( em -> {
			SpecialItem item = em.createNativeQuery(
							"SELECT * FROM special_table WHERE long_list = :longList", SpecialItem.class )
					.setParameter( "longList", longList )
					.getSingleResult();

			assertEquals( "LongList", item.getName() );
		} );
	}

	@Entity(name = "SpecialItem")
	@Table(name = "special_table")
	public static class SpecialItem implements Serializable {

		@Id
		@Column(length = 30)
		private String name;

		@Column(name = "long_list", columnDefinition = "VARCHAR(255)")
		private LongList longList;

		public SpecialItem() {
		}

		public SpecialItem(String name, LongList longList) {
			this.name = name;
			this.longList = longList;
		}

		public LongList getLongList() {
			return longList;
		}

		public void setLongList(LongList longList) {
			this.longList = longList;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class LongList extends java.util.ArrayList<Long> {

		public LongList() {
			super();
		}

		public LongList(int initialCapacity) {
			super( initialCapacity );
		}

		public LongList(Long... longs) {
			super( longs.length );
			this.addAll( Arrays.asList( longs ) );
		}
	}

	public static class StringifiedCollectionTypeContributor implements TypeContributor {

		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeJavaType( StringifiedCollectionJavaType.INSTANCE );
			typeContributions.contributeType( StringifiedCollectionType.INSTANCE );
		}

		private static class StringifiedCollectionType
				extends AbstractSingleColumnStandardBasicType<LongList> {

			private final String[] regKeys;
			private final String name;

			public static final StringifiedCollectionType INSTANCE = new StringifiedCollectionType();

			public StringifiedCollectionType() {
				super(
						LongVarcharJdbcType.INSTANCE,
						StringifiedCollectionJavaType.INSTANCE
				);
				regKeys = new String[] { LongList.class.getName() };
				name = "StringifiedCollection";
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String[] getRegistrationKeys() {
				return regKeys.clone();
			}

			@Override
			protected boolean registerUnderJavaType() {
				return true;
			}
		}

		private static class StringifiedCollectionJavaType extends AbstractClassJavaType<LongList> {

			public static StringifiedCollectionJavaType INSTANCE = new StringifiedCollectionJavaType();

			public StringifiedCollectionJavaType() {
				super( LongList.class );
			}

			@Override
			public String toString(LongList value) {
				if ( value == null ) {
					return "null";
				}
				StringBuilder sb = new StringBuilder();
				sb.append( '[' );
				String glue = "";
				for ( Long v : value ) {
					sb.append( glue ).append( v );
					glue = ",";
				}
				sb.append( ']' );
				return sb.toString();
			}

			@Override
			public LongList fromString(CharSequence sequence) {
				if ( sequence == null ) {
					return null;
				}
				final String string = sequence.toString();
				if ( "null".equals( string ) ) {
					return null;
				}

				if ( string.length() <= 2 ) {
					return new LongList();
				}

				String[] parts = string.substring( 1, string.length() - 1 ).split( "," );
				LongList results = new LongList( parts.length );

				for ( String part : parts ) {
					results.add( Long.valueOf( part ) );
				}

				return results;
			}

			@Override
			public <X> X unwrap(LongList value, Class<X> type, WrapperOptions options) {
				if ( value == null ) {
					return null;
				}

				if ( String.class.isAssignableFrom( type ) ) {
					return (X) this.toString( value );
				}

				throw unknownUnwrap( type );
			}

			@Override
			public <X> LongList wrap(X value, WrapperOptions options) {
				if ( value == null ) {
					return null;
				}

				Class type = value.getClass();

				if ( String.class.isAssignableFrom( type ) ) {
					String s = (String) value;
					return this.fromString( s );
				}

				throw unknownWrap( type );
			}
		}
	}
}
