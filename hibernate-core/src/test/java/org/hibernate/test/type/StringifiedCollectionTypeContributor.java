/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.util.Collection;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;

/**
 *
 * @author Yordan Gigov
 */
public class StringifiedCollectionTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( StringifiedCollectionJavaTypeDescriptor.INSTANCE );
		typeContributions.contributeType( StringifiedCollectionType.INSTANCE );
	}

	public static class LongList extends java.util.ArrayList<Long> {
		private static final long serialVersionUID = 6621718915733197817L;
		public LongList() {
			super();
		}
		public LongList(int initialCapacity) {
			super( initialCapacity );
		}
		public LongList(Collection<Long> initalContents) {
			super( initalContents );
		}
	}

	private static class StringifiedCollectionType 
		extends AbstractSingleColumnStandardBasicType<LongList>  {

		private static final long serialVersionUID = -5209685045343388046L;
		private final String[] regKeys;
		private final String name;

		public static final StringifiedCollectionType INSTANCE = new StringifiedCollectionType();

		public StringifiedCollectionType() {
			super( org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor.INSTANCE,
					StringifiedCollectionJavaTypeDescriptor.INSTANCE );
			regKeys = new String[] {
				LongList.class.getName(),
			};
			name = "StringifiedCollection";
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String[] getRegistrationKeys() {
			return (String[]) regKeys.clone();
		}

		@Override
		protected boolean registerUnderJavaType() {
			return true;
		}
	}

	private static class StringifiedCollectionJavaTypeDescriptor extends AbstractTypeDescriptor<LongList> {
		public static StringifiedCollectionJavaTypeDescriptor INSTANCE = new StringifiedCollectionJavaTypeDescriptor();
		private static final long serialVersionUID = 1112405740480736262L;

		public StringifiedCollectionJavaTypeDescriptor() {
			super( LongList.class );
		}

		@Override
		public String toString(LongList value) {
			if ( value == null ) {
				return "null";
			}
			StringBuilder sb = new StringBuilder();
			sb.append('[');
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
