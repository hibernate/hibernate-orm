/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;


import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

import static org.hibernate.dialect.StructHelper.getEmbeddedPart;
import static org.hibernate.dialect.StructHelper.instantiate;

/**
 * A Helper for serializing and deserializing XML, based on an {@link EmbeddableMappingType}.
 */
@Internal
public class XmlHelper {

	/**
	 * The root tag under which values are placed as XML elements.
	 */
	public static final String ROOT_TAG = "e";
	private static final String START_TAG = "<" + ROOT_TAG + ">";
	private static final String END_TAG = "</" + ROOT_TAG + ">";

	private static Object fromEscapedString(
			JdbcMapping jdbcMapping,
			String string,
			int start,
			int end) {
		final String unescaped = unescape( string, start, end );
		return fromString( jdbcMapping, unescaped, 0, unescaped.length() );
	}

	private static Object fromString(
			JdbcMapping jdbcMapping,
			String string,
			int start,
			int end) {
		return jdbcMapping.getJdbcJavaType().fromEncodedString(
				string,
				start,
				end
		);
	}

	private static Object fromRawObject(
			JdbcMapping jdbcMapping,
			Object raw,
			WrapperOptions options) {
		return jdbcMapping.getJdbcJavaType().wrap(
				raw,
				options
		);
	}

	private static String unescape(String string, int start, int end) {
		final StringBuilder sb = new StringBuilder( end - start );
		for ( int i = start; i < end; i++ ) {
			final char c = string.charAt( i );
			OUTER: switch ( c ) {
				case '<':
					sb.append( "&lt;" );
					break;
				case '&':
					// It must be &amp; or &lt;
					if ( i + 3 < end ) {
						final char c1 = string.charAt( i + 1 );
						switch ( c1 ) {
							case 'l':
								if ( string.charAt( i + 2 ) == 't' && string.charAt( i + 3 ) == ';' ) {
									sb.append( '<' );
									i += 3;
								}
								break OUTER;
							case 'a':
								if ( i + 4 < end
										&& string.charAt( i + 2 ) == 'm'
										&& string.charAt( i + 3 ) == 'p'
										&& string.charAt( i + 4 ) == ';' ) {
									sb.append( '&' );
									i += 4;
								}
								break OUTER;
							case 'g':
								if ( string.charAt( i + 2 ) == 't' && string.charAt( i + 3 ) == ';' ) {
									sb.append( '>' );
									i += 3;
								}
								break OUTER;
						}
					}
					throw new IllegalArgumentException( "Illegal XML content: " + string.substring( start, end ) );
				default:
					sb.append( c );
					break;
			}
		}
		return sb.toString();
	}

	private static Object fromString(
			EmbeddableMappingType embeddableMappingType,
			String string,
			WrapperOptions options,
			int selectableIndex,
			int start,
			int end) {
		final JdbcMapping jdbcMapping = embeddableMappingType.getJdbcValueSelectable( selectableIndex ).getJdbcMapping();
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.BOOLEAN:
			case SqlTypes.BIT:
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
			case SqlTypes.BIGINT:
			case SqlTypes.FLOAT:
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
			case SqlTypes.DECIMAL:
			case SqlTypes.NUMERIC:
				Class<?> javaTypeClass = jdbcMapping.getMappedJavaType().getJavaTypeClass();
				if ( javaTypeClass.isEnum() ) {
					return javaTypeClass.getEnumConstants()
							[IntegerJavaType.INSTANCE.fromEncodedString( string, start, end )];
				}
				return fromString(
						jdbcMapping,
						string,
						start,
						end
				);
			case SqlTypes.DATE:
				return fromRawObject(
						jdbcMapping,
						JdbcDateJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				return fromRawObject(
						jdbcMapping,
						JdbcTimeJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TIMESTAMP:
				return fromRawObject(
						jdbcMapping,
						JdbcTimestampJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				return fromRawObject(
						jdbcMapping,
						OffsetDateTimeJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
			case SqlTypes.UUID:
				return fromRawObject(
						jdbcMapping,
						Base64.getDecoder().decode( string.substring( start, end ) ),
						options
				);
			default:
				return fromEscapedString(
						jdbcMapping,
						string,
						start,
						end
				);
		}
	}

	public static <X> X fromString(
			EmbeddableMappingType embeddableMappingType,
			String string,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		if ( !string.startsWith( START_TAG ) || !string.endsWith( END_TAG ) ) {
			throw new IllegalArgumentException( "Illegal XML for struct: " + string );
		}
		int end;
		final Object[] array;
		if ( embeddableMappingType == null ) {
			assert !returnEmbeddable;
			final List<Object> values = new ArrayList<>( 8 );
			end = fromString( string, values, START_TAG.length() );
			array = values.toArray();
		}
		else {
			array = new Object[embeddableMappingType.getJdbcValueCount() + ( embeddableMappingType.isPolymorphic() ? 1 : 0 )];
			end = fromString( embeddableMappingType, string, returnEmbeddable, options, array, START_TAG.length() );
		}
		assert end + END_TAG.length() == string.length();

		if ( returnEmbeddable ) {
			final StructAttributeValues attributeValues = StructHelper.getAttributeValues( embeddableMappingType, array, options );
			//noinspection unchecked
			return (X) instantiate( embeddableMappingType, attributeValues, options.getSessionFactory() );
		}
		//noinspection unchecked
		return (X) array;
	}

	private static int fromString(
			String string,
			List<Object> values,
			int start) {
		int tagNameStart = -1;
		int contentStart = -1;
		String tagName = null;
		for ( int i = start; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			switch ( c ) {
				case '<':
					if ( tagNameStart == -1 ) {
						if ( string.charAt( i + 1 ) == '/' ) {
							// This is the parent closing tag, so we stop here
							assert tagName == null;
							assert contentStart == -1;
							return i;
						}
						// A start tag
						tagNameStart = i + 1;
					}
					else {
						assert contentStart != -1;
						if ( string.charAt( i + 1 ) == '/' ) {
							// This is a closing tag
							assert tagName != null;
							values.add( unescape( string, contentStart, i ) );
						}
						else {
							// Nested tag
							final List<Object> subValues = new ArrayList<>( 8 );
							final int end = fromString( string, subValues, i );
							values.add( subValues.toArray() );
							// The end is the start angle bracket for the end tag
							assert string.charAt( end ) == '<';
							assert string.charAt( end + 1 ) == '/';
							assert string.regionMatches( end + 2, tagName, 0, tagName.length() );
							i = end;
						}
						// consume the whole closing tag
						i += tagName.length() + 2;
						tagNameStart = -1;
						contentStart = -1;
						tagName = null;
					}
					break;
				case '>':
					if ( tagName == null ) {
						// The closing angle bracket of the start tag
						assert contentStart == -1;
						assert tagNameStart != -1;
						tagName = string.substring( tagNameStart, i );
						contentStart = i + 1;
					}
					else {
						// This must be a char in the content
						assert contentStart != -1;
					}
					break;
				case '/':
					if ( tagName == null ) {
						// A shorthand tag encodes null
						values.add( null );
						// skip the closing angle bracket
						i++;
						tagNameStart = -1;
						assert string.charAt( i ) == '>';
					}
					else {
						// This must be a char in the content
						assert contentStart != -1;
					}
					break;
			}
		}
		throw new IllegalArgumentException( "XML not properly formed: " + string.substring( start ) );
	}

	private static int fromString(
			EmbeddableMappingType embeddableMappingType,
			String string,
			boolean returnEmbeddable,
			WrapperOptions options,
			Object[] values,
			int start) throws SQLException {
		int tagNameStart = -1;
		int contentStart = -1;
		String tagName = null;
		for ( int i = start; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			switch ( c ) {
				case '<':
					if ( tagNameStart == -1 ) {
						if ( string.charAt( i + 1 ) == '/' ) {
							// This is the parent closing tag, so we stop here
							assert tagName == null;
							assert contentStart == -1;
							return i;
						}
						// A start tag
						tagNameStart = i + 1;
					}
					else {
						assert contentStart != -1;
						if ( string.charAt( i + 1 ) == '/' ) {
							// This is a closing tag
							assert tagName != null;
							final int selectableMapping = getSelectableMapping( embeddableMappingType, tagName );
							values[selectableMapping] = fromString(
									embeddableMappingType,
									string,
									options,
									selectableMapping,
									contentStart,
									i
							);
						}
						else {
							// Nested tag
							final int selectableIndex = getSelectableMapping( embeddableMappingType, tagName );
							final SelectableMapping selectable = embeddableMappingType.getJdbcValueSelectable(
									selectableIndex
							);
							if ( !( selectable.getJdbcMapping().getJdbcType() instanceof AggregateJdbcType ) ) {
								throw new IllegalArgumentException(
										String.format(
												"XML starts sub-object for a non-aggregate type at index %d. Selectable [%s] is of type [%s]",
												i,
												selectable.getSelectableName(),
												selectable.getJdbcMapping().getJdbcType().getClass().getName()
										)
								);
							}
							final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) selectable.getJdbcMapping().getJdbcType();
							final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
							final Object[] subValues;
							final int end;
							if ( aggregateJdbcType.getJdbcTypeCode() == SqlTypes.SQLXML || aggregateJdbcType.getDefaultSqlTypeCode() == SqlTypes.SQLXML ) {
								// If we stay in XML land, we can recurse instead
								subValues = new Object[subMappingType.getJdbcValueCount()];
								end = fromString(
										subMappingType,
										string,
										returnEmbeddable,
										options,
										subValues,
										i
								);
							}
							else {
								// Determine the end of the XML element
								while ( string.charAt( i ) != '<' ) {
									i++;
								}
								end = i;
								subValues = aggregateJdbcType.extractJdbcValues(
										CharSequenceHelper.subSequence(
												string,
												start,
												end
										),
										options
								);
							}
							if ( returnEmbeddable ) {
								final StructAttributeValues attributeValues = StructHelper.getAttributeValues(
										subMappingType,
										subValues,
										options
								);
								values[selectableIndex] = instantiate( subMappingType, attributeValues, options.getSessionFactory() );
							}
							else {
								values[selectableIndex] = subValues;
							}
							// The end is the start angle bracket for the end tag
							assert string.charAt( end ) == '<';
							assert string.charAt( end + 1 ) == '/';
							assert string.regionMatches( end + 2, tagName, 0, tagName.length() );
							i = end;
						}
						// consume the whole closing tag
						i += tagName.length() + 2;
						tagNameStart = -1;
						contentStart = -1;
						tagName = null;
					}
					break;
				case '>':
					if ( tagName == null ) {
						// The closing angle bracket of the start tag
						assert contentStart == -1;
						assert tagNameStart != -1;
						tagName = string.substring( tagNameStart, i );
						contentStart = i + 1;
					}
					else {
						// This must be a char in the content
						assert contentStart != -1;
					}
					break;
				case '/':
					if ( tagName == null ) {
						// A shorthand tag encodes null,
						// but we don't have to do anything because null is the default.
						// Also, skip the closing angle bracket
						i++;
						tagNameStart = -1;
						assert string.charAt( i ) == '>';
					}
					else {
						// This must be a char in the content
						assert contentStart != -1;
					}
					break;
			}
		}
		throw new IllegalArgumentException( "XML not properly formed: " + string.substring( start ) );
	}

	public static String toString(
			EmbeddableMappingType embeddableMappingType,
			Object value,
			WrapperOptions options) {
		final StringBuilder sb = new StringBuilder();
		sb.append( START_TAG );
		toString( embeddableMappingType, value, options, new XMLAppender( sb ) );
		sb.append( END_TAG );
		return sb.toString();
	}

	private static void toString(
			EmbeddableMappingType embeddableMappingType,
			Object value,
			WrapperOptions options,
			XMLAppender sb) {
		final Object[] array = embeddableMappingType.getValues( value );
		final int numberOfAttributes = embeddableMappingType.getNumberOfAttributeMappings();
		for ( int i = 0; i < array.length; i++ ) {
			if ( array[i] == null ) {
				continue;
			}
			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
			if ( attributeMapping instanceof SelectableMapping ) {
				final SelectableMapping selectable = (SelectableMapping) attributeMapping;
				final String tagName = selectable.getSelectableName();
				sb.append( '<' );
				sb.append( tagName );
				sb.append( '>' );
				serializeValueTo( sb, selectable, array[i], options );
				sb.append( '<' );
				sb.append( '/' );
				sb.append( tagName );
				sb.append( '>' );
			}
			else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
				final EmbeddableMappingType mappingType = (EmbeddableMappingType) attributeMapping.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if ( aggregateMapping == null ) {
					toString(
							mappingType,
							array[i],
							options,
							sb
					);
				}
				else {
					final String tagName = aggregateMapping.getSelectableName();
					sb.append( '<' );
					sb.append( tagName );
					sb.append( '>' );
					toString(
							mappingType,
							array[i],
							options,
							sb
					);
					sb.append( '<' );
					sb.append( '/' );
					sb.append( tagName );
					sb.append( '>' );
				}
			}
			else {
				throw new UnsupportedOperationException( "Unsupported attribute mapping: " + attributeMapping );
			}
		}
	}

	private static void serializeValueTo(XMLAppender appender, SelectableMapping selectable, Object value, WrapperOptions options) {
		final JdbcMapping jdbcMapping = selectable.getJdbcMapping();
		//noinspection unchecked
		final JavaType<Object> jdbcJavaType = (JavaType<Object>) jdbcMapping.getJdbcJavaType();
		final Object relationalValue = jdbcMapping.convertToRelationalValue( value );
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( relationalValue instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( (Boolean) relationalValue ? '1' : '0' );
					break;
				}
			case SqlTypes.BOOLEAN:
			case SqlTypes.BIT:
			case SqlTypes.BIGINT:
			case SqlTypes.FLOAT:
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
			case SqlTypes.DECIMAL:
			case SqlTypes.NUMERIC:
			case SqlTypes.DURATION:
				jdbcJavaType.appendEncodedString(
						appender,
						jdbcJavaType.unwrap(
								relationalValue,
								jdbcJavaType.getJavaTypeClass(),
								options
						)
				);
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( relationalValue instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( (Boolean) relationalValue ? 'Y' : 'N' );
					break;
				}
			case SqlTypes.LONGVARCHAR:
			case SqlTypes.LONGNVARCHAR:
			case SqlTypes.LONG32VARCHAR:
			case SqlTypes.LONG32NVARCHAR:
				appender.startEscaping();
				jdbcJavaType.appendEncodedString(
						appender,
						jdbcJavaType.unwrap(
								relationalValue,
								jdbcJavaType.getJavaTypeClass(),
								options
						)
				);
				appender.endEscaping();
				break;
			case SqlTypes.DATE:
				JdbcDateJavaType.INSTANCE.appendEncodedString(
						appender,
						jdbcJavaType.unwrap( value, java.sql.Date.class, options )
				);
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				JdbcTimeJavaType.INSTANCE.appendEncodedString(
						appender,
						jdbcJavaType.unwrap( value, java.sql.Time.class, options )
				);
				break;
			case SqlTypes.TIMESTAMP:
				JdbcTimestampJavaType.INSTANCE.appendEncodedString(
						appender,
						jdbcJavaType.unwrap( value, java.sql.Timestamp.class, options )
				);
				break;
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				DateTimeFormatter.ISO_OFFSET_DATE_TIME.formatTo( jdbcJavaType.unwrap( value, OffsetDateTime.class, options ), appender );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
			case SqlTypes.UUID:
				appender.writeBase64( jdbcJavaType.unwrap( relationalValue, byte[].class, options ) );
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in struct: " + jdbcMapping.getJdbcType() );
		}
	}

	private static int getSelectableMapping(
			EmbeddableMappingType embeddableMappingType,
			String name) {
		final int selectableIndex = embeddableMappingType.getSelectableIndex( name );
		if ( selectableIndex == -1 ) {
			throw new IllegalArgumentException(
					String.format(
							"Could not find selectable [%s] in embeddable type [%s] for JSON processing.",
							name,
							embeddableMappingType.getMappedJavaType().getJavaTypeClass().getName()
					)
			);
		}
		return selectableIndex;
	}

	private static class XMLAppender extends OutputStream implements SqlAppender {

		private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
		private final StringBuilder sb;
		private boolean escape;
		private OutputStream base64OutputStream;

		public XMLAppender(StringBuilder sb) {
			this.sb = sb;
		}

		@Override
		public void appendSql(String fragment) {
			append( fragment );
		}

		@Override
		public void appendSql(char fragment) {
			append( fragment );
		}

		@Override
		public void appendSql(int value) {
			sb.append( value );
		}

		@Override
		public void appendSql(long value) {
			sb.append( value );
		}

		@Override
		public void appendSql(boolean value) {
			sb.append( value );
		}

		@Override
		public String toString() {
			return sb.toString();
		}

		public void startEscaping() {
			assert !escape;
			escape = true;
		}

		public void endEscaping() {
			assert escape;
			escape = false;
		}

		@Override
		public XMLAppender append(char fragment) {
			if ( escape ) {
				appendEscaped( fragment );
			}
			else {
				sb.append( fragment );
			}
			return this;
		}

		@Override
		public XMLAppender append(CharSequence csq) {
			return append( csq, 0, csq.length() );
		}

		@Override
		public XMLAppender append(CharSequence csq, int start, int end) {
			if ( escape ) {
				int len = end - start;
				sb.ensureCapacity( sb.length() + len );
				for ( int i = start; i < end; i++ ) {
					appendEscaped( csq.charAt( i ) );
				}
			}
			else {
				sb.append( csq, start, end );
			}
			return this;
		}

		private void appendEscaped(char fragment) {
			switch ( fragment ) {
				case '<':
					sb.append( "&lt;" );
					break;
				case '&':
					sb.append( "&amp;" );
					break;
				default:
					sb.append( fragment );
					break;
			}
		}

		@Override
		public void write(int v) {
			final String hex = Integer.toHexString( v );
			sb.ensureCapacity( sb.length() + hex.length() + 1 );
			if ( ( hex.length() & 1 ) == 1 ) {
				sb.append( '0' );
			}
			sb.append( hex );
		}

		@Override
		public void write(byte[] bytes) {
			write(bytes, 0, bytes.length);
		}

		@Override
		public void write(byte[] bytes, int off, int len) {
			sb.ensureCapacity( sb.length() + ( len << 1 ) );
			for ( int i = 0; i < len; i++ ) {
				final int v = bytes[off + i] & 0xFF;
				sb.append( HEX_ARRAY[v >>> 4] );
				sb.append( HEX_ARRAY[v & 0x0F] );
			}
		}

		public void writeBase64(byte[] bytes) {
			if ( base64OutputStream == null ) {
				base64OutputStream = Base64.getEncoder().wrap( this );
			}
			try {
				base64OutputStream.write( bytes );
			}
			catch (IOException e) {
				// Should never happen
				throw new RuntimeException( e );
			}
		}
	}

}
