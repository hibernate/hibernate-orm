/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

public class LongListTypeContributorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				SpecialItem.class,
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		entityManagerFactory().getMetamodel().getTypeConfiguration().getJavaTypeDescriptorRegistry()
				.addDescriptor( StringifiedCollectionTypeContributor.StringifiedCollectionJavaTypeDescriptor.INSTANCE );
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( "hibernate.type_contributors", (TypeContributorList) () -> Arrays.asList(
			new StringifiedCollectionTypeContributor()
		) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11409")
	public void testParameterRegisterredCollection() {

		LongList longList = new LongList(5L, 11L, 6123L, -61235L, 24L);

		doInJPA( this::entityManagerFactory, em -> {
			SpecialItem item = new SpecialItem( "LongList", longList );
			em.persist( item );
		} );

		doInJPA( this::entityManagerFactory, em -> {

			SpecialItem item = (SpecialItem) em.createNativeQuery(
				"SELECT * FROM special_table WHERE longList = ?", SpecialItem.class )
			.setParameter(1, longList)
			.getSingleResult();

			assertEquals( "LongList", item.getName() );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			SpecialItem item = (SpecialItem) em.createNativeQuery(
				"SELECT * FROM special_table WHERE longList = :longList", SpecialItem.class )
			.setParameter("longList", longList)
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

		@Column(columnDefinition = "VARCHAR(255)")
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
			for ( Long l : longs ) {
				this.add( l );
			}
		}
	}

	public static class StringifiedCollectionTypeContributor implements TypeContributor {

		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
//			JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( StringifiedCollectionJavaTypeDescriptor.INSTANCE );
			typeContributions.contributeType( StringifiedCollectionType.INSTANCE );
		}

		private static class StringifiedCollectionType
				extends AbstractSingleColumnStandardBasicType<LongList> {

			private final String[] regKeys;
			private final String name;

			public static final StringifiedCollectionType INSTANCE = new StringifiedCollectionType();

			public StringifiedCollectionType() {
				super( org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor.INSTANCE,
					   StringifiedCollectionJavaTypeDescriptor.INSTANCE );
				regKeys = new String[]{ LongList.class.getName() };
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

		private static class StringifiedCollectionJavaTypeDescriptor extends AbstractTypeDescriptor<LongList> {

			public static StringifiedCollectionJavaTypeDescriptor INSTANCE = new StringifiedCollectionJavaTypeDescriptor();

			public StringifiedCollectionJavaTypeDescriptor() {
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
			public LongList fromString(String string) {
				if ( string == null || "null".equals( string ) ) {
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
