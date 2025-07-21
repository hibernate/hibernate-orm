/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;


import java.io.OutputStream;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Internal;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;

import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;
import static org.hibernate.type.descriptor.jdbc.StructHelper.getEmbeddedPart;
import static org.hibernate.type.descriptor.jdbc.StructHelper.instantiate;

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
	private static final String NULL_TAG = "<" + ROOT_TAG + "/>";
	private static final String COLLECTION_START_TAG = "<Collection>";
	private static final String COLLECTION_END_TAG = "</Collection>";
	private static final String EMPTY_COLLECTION_TAG = "<Collection/>";

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
								else if ( i + 5 < end
									&& string.charAt( i + 2 ) == 'p'
									&& string.charAt( i + 3 ) == 'o'
									&& string.charAt( i + 4 ) == 's'
									&& string.charAt( i + 5 ) == ';' ) {
									sb.append( '\'' );
									i += 5;
								}
								break OUTER;
							case 'g':
								if ( string.charAt( i + 2 ) == 't' && string.charAt( i + 3 ) == ';' ) {
									sb.append( '>' );
									i += 3;
								}
								break OUTER;
							case 'q':
								if ( i + 5 < end
									&& string.charAt( i + 2 ) == 'u'
									&& string.charAt( i + 3 ) == 'o'
									&& string.charAt( i + 4 ) == 't'
									&& string.charAt( i + 5 ) == ';' ) {
									sb.append( '"' );
									i += 5;
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
			boolean returnEmbeddable,
			WrapperOptions options,
			int selectableIndex,
			int start,
			int end) throws SQLException {
		final JdbcMapping jdbcMapping = embeddableMappingType.getJdbcValueSelectable( selectableIndex )
				.getJdbcMapping();
		return fromString(
				jdbcMapping.getMappedJavaType(),
				jdbcMapping.getJdbcJavaType(),
				jdbcMapping.getJdbcType(),
				string,
				returnEmbeddable,
				options,
				start,
				end
		);
	}

	private static Object fromString(
			JavaType<?> javaType,
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType,
			String string,
			boolean returnEmbeddable,
			WrapperOptions options,
			int start,
			int end) throws SQLException {
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( jdbcJavaType.getJavaTypeClass() == Boolean.class ) {
					return jdbcJavaType.wrap( Integer.parseInt( string, start, end, 10 ), options );
				}
				else if ( jdbcJavaType instanceof EnumJavaType<?> ) {
					return jdbcJavaType.wrap( Integer.parseInt( string, start, end, 10 ), options );
				}
			case SqlTypes.BOOLEAN:
			case SqlTypes.BIT:
			case SqlTypes.BIGINT:
			case SqlTypes.FLOAT:
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
			case SqlTypes.DECIMAL:
			case SqlTypes.NUMERIC:
				return jdbcJavaType.fromEncodedString(
						string,
						start,
						end
				);
			case SqlTypes.DATE:
				return jdbcJavaType.wrap(
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
				return jdbcJavaType.wrap(
						JdbcTimeJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TIMESTAMP:
				return jdbcJavaType.wrap(
						JdbcTimestampJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				return jdbcJavaType.wrap(
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
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:
				return jdbcJavaType.wrap(
						PrimitiveByteArrayJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.UUID:
				return jdbcJavaType.wrap(
						PrimitiveByteArrayJavaType.INSTANCE.fromString(
								string.substring( start, end ).replace( "-", "" )
						),
						options
				);
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( jdbcJavaType.getJavaTypeClass() == Boolean.class && end == start + 1 ) {
					return jdbcJavaType.wrap( string.charAt( start ), options );
				}
			default:
				if ( jdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
					final Object[] subValues = aggregateJdbcType.extractJdbcValues(
							CharSequenceHelper.subSequence(
									string,
									start,
									end
							),
							options
					);
					if ( returnEmbeddable ) {
						final StructAttributeValues subAttributeValues = StructHelper.getAttributeValues(
								aggregateJdbcType.getEmbeddableMappingType(),
								subValues,
								options
						);
						return instantiate( aggregateJdbcType.getEmbeddableMappingType(), subAttributeValues ) ;
					}
					return subValues;
				}
				final String unescaped = unescape( string, start, end );
				return jdbcJavaType.fromEncodedString( unescaped, 0, unescaped.length() );
		}
	}

	public static <X> X fromString(
			EmbeddableMappingType embeddableMappingType,
			String string,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		if ( NULL_TAG.equals( string ) ) {
			return null;
		}
		int contentEnd = string.length() - 1;
		while ( contentEnd >= 0 ) {
			if ( !Character.isWhitespace( string.charAt( contentEnd ) ) ) {
				break;
			}
			contentEnd--;
		}
		if ( !string.startsWith( START_TAG ) || !string.regionMatches( contentEnd - END_TAG.length()+ 1, END_TAG, 0, END_TAG.length() ) ) {
			throw new IllegalArgumentException( "XML not properly formatted: " + string );
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
		assert end + END_TAG.length() == contentEnd + 1;

		if ( returnEmbeddable ) {
			final StructAttributeValues attributeValues = StructHelper.getAttributeValues( embeddableMappingType, array, options );
			//noinspection unchecked
			return (X) instantiate( embeddableMappingType, attributeValues );
		}
		//noinspection unchecked
		return (X) array;
	}

	public static <X> X arrayFromString(
			JavaType<X> javaType,
			XmlArrayJdbcType xmlArrayJdbcType,
			String string,
			WrapperOptions options) throws SQLException {
		if ( string == null ) {
			return null;
		}
		else if ( EMPTY_COLLECTION_TAG.equals( string ) ) {
			return javaType.wrap( Collections.emptyList(), options );
		}
		else if ( !string.startsWith( COLLECTION_START_TAG ) || !string.endsWith( COLLECTION_END_TAG ) ) {
			throw new IllegalArgumentException( "Illegal XML for array: " + string );
		}
		final JavaType<?> elementJavaType = ((BasicPluralJavaType<?>) javaType).getElementJavaType();
		final Class<?> preferredJavaTypeClass = xmlArrayJdbcType.getElementJdbcType().getPreferredJavaTypeClass( options );
		final JavaType<?> jdbcJavaType;
		if ( preferredJavaTypeClass == null || preferredJavaTypeClass == elementJavaType.getJavaTypeClass() ) {
			jdbcJavaType = elementJavaType;
		}
		else {
			jdbcJavaType = options.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( preferredJavaTypeClass );
		}
		final ArrayList<Object> arrayList = new ArrayList<>();
		final int end = fromArrayString(
				string,
				false,
				options,
				COLLECTION_START_TAG.length(),
				arrayList,
				elementJavaType,
				jdbcJavaType,
				xmlArrayJdbcType.getElementJdbcType()
		);
		assert end + COLLECTION_END_TAG.length() == string.length();
		return javaType.wrap( arrayList, options );
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
									returnEmbeddable,
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
							final JdbcType jdbcType = selectable.getJdbcMapping().getJdbcType();
							if ( jdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
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
									values[selectableIndex] = instantiate( subMappingType, attributeValues );
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
							else if ( selectable.getJdbcMapping() instanceof BasicPluralType<?,?> pluralType ) {
								final BasicType<?> elementType = pluralType.getElementType();
								final ArrayList<Object> arrayList = new ArrayList<>();
								final int end = fromArrayString(
										string,
										returnEmbeddable,
										options,
										i,
										arrayList,
										elementType.getMappedJavaType(),
										elementType.getJdbcJavaType(),
										elementType.getJdbcType()
								);
								values[selectableIndex] = selectable.getJdbcMapping().getJdbcJavaType().wrap( arrayList, options );
								// The end is the start angle bracket for the end tag
								assert string.charAt( end ) == '<';
								assert string.charAt( end + 1 ) == '/';
								assert string.regionMatches( end + 2, tagName, 0, tagName.length() );
								i = end;
							}
							else {
								throw new IllegalArgumentException(
										String.format(
												"XML starts sub-object for a non-aggregate type at index %d. Selectable [%s] is of type [%s]",
												i,
												selectable.getSelectableName(),
												jdbcType.getClass().getName()
										)
								);
							}
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

	private static int fromArrayString(
			String string,
			boolean returnEmbeddable,
			WrapperOptions options,
			int start,
			ArrayList<Object> arrayList,
			JavaType<?> javaType,
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType) throws SQLException {
		int tagNameStart = -1;
		int contentStart = -1;
		for ( int i = start; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			switch ( c ) {
				case '<':
					if ( tagNameStart == -1 ) {
						if ( string.charAt( i + 1 ) == '/' ) {
							// This is the parent closing tag, so we stop here
							assert contentStart == -1;
							return i;
						}
						// A start tag
						tagNameStart = i + 1;
					}
					else {
						if ( string.charAt( i + 1 ) == '/' ) {
							// This is a closing tag
							if ( !string.regionMatches( i + 2, ROOT_TAG + ">", 0, ROOT_TAG.length() + 1 ) ) {
								throw new IllegalArgumentException( "XML not properly formed: " + string.substring( start ) );
							}
							arrayList.add( fromString(
									javaType,
									jdbcJavaType,
									jdbcType,
									string,
									returnEmbeddable,
									options,
									contentStart,
									i
							) );
						}
						else {
							// Nested tag
							if ( jdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
								final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
								final Object[] array = new Object[embeddableMappingType.getJdbcValueCount() + ( embeddableMappingType.isPolymorphic() ? 1 : 0 )];
								final int end = fromString( embeddableMappingType, string, returnEmbeddable, options, array, contentStart );

								if ( returnEmbeddable ) {
									final StructAttributeValues attributeValues =
											StructHelper.getAttributeValues( embeddableMappingType, array, options );
									arrayList.add( instantiate( embeddableMappingType, attributeValues ) );
								}
								else {
									arrayList.add( array );
								}
								i = end + 1;
							}
							else {
								throw new IllegalArgumentException( "XML not properly formed: " + string.substring( start ) );
							}
						}
						// consume the whole closing tag
						i += ROOT_TAG.length() + 2;
						tagNameStart = -1;
						contentStart = -1;
					}
					break;
				case '>':
					if ( contentStart == -1 ) {
						// The closing angle bracket of the start tag
						assert tagNameStart != -1;
						if ( !ROOT_TAG.equals( string.substring( tagNameStart, i ) ) ) {
							throw new IllegalArgumentException( "XML not properly formed: " + string.substring( start ) );
						}
						contentStart = i + 1;
					}
					else {
						// This must be a char in the content
					}
					break;
				case '/':
					if ( contentStart == -1 ) {
						// A shorthand tag encodes null
						// Also, skip the closing angle bracket
						arrayList.add( null );
						i++;
						tagNameStart = -1;
						assert string.charAt( i ) == '>';
					}
					else {
						// This must be a char in the content
					}
					break;
			}
		}
		throw new IllegalArgumentException( "XML not properly formed: " + string.substring( start ) );
	}

	public static String toString(
			EmbeddableMappingType embeddableMappingType,
			Object value,
			WrapperOptions options) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append( START_TAG );
		toString( embeddableMappingType, embeddableMappingType.getValues( value ), options, new XMLAppender( sb ) );
		sb.append( END_TAG );
		return sb.toString();
	}

	public static String arrayToString(EmbeddableMappingType elementMappingType, Object[] values, WrapperOptions options) {
		if ( values.length == 0 ) {
			return EMPTY_COLLECTION_TAG;
		}
		final StringBuilder sb = new StringBuilder();
		final XMLAppender xmlAppender = new XMLAppender( sb );
		sb.append( COLLECTION_START_TAG );
		for ( Object value : values ) {
			if ( value == null ) {
				sb.append( NULL_TAG );
			}
			else {
				sb.append( START_TAG );
				toString( elementMappingType, elementMappingType.getValues( value ), options, xmlAppender );
				sb.append( END_TAG );
			}
		}
		sb.append( COLLECTION_END_TAG );
		return sb.toString();
	}

	public static String arrayToString(
			JavaType<?> elementJavaType,
			JdbcType elementJdbcType,
			Object[] values,
			WrapperOptions options) {
		if ( values.length == 0 ) {
			return EMPTY_COLLECTION_TAG;
		}
		final StringBuilder sb = new StringBuilder();
		final XMLAppender xmlAppender = new XMLAppender( sb );
		sb.append( COLLECTION_START_TAG );
		for ( Object value : values ) {
			if ( value == null ) {
				sb.append( NULL_TAG );
			}
			else {
				sb.append( START_TAG );
				//noinspection unchecked
				convertedBasicValueToString( xmlAppender, value, options, (JavaType<Object>) elementJavaType, elementJdbcType );
				sb.append( END_TAG );
			}
		}
		sb.append( COLLECTION_END_TAG );
		return sb.toString();
	}

	private static void toString(
			EmbeddableMappingType embeddableMappingType,
			@Nullable Object[] attributeValues,
			WrapperOptions options,
			XMLAppender sb) {
		// Always append all the nodes, even if the value is null.
		// This is done in order to allow using xmlextract/xmlextractvalue
		// which fail if a XPath expression does not resolve to a result
		final int attributeCount = embeddableMappingType.getNumberOfAttributeMappings();
		for ( int i = 0; i < attributeCount; i++ ) {
			final Object attributeValue = attributeValues == null ? null : attributeValues[i];
			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
			if ( attributeMapping instanceof SelectableMapping selectable ) {
				final String tagName = selectable.getSelectableName();
				sb.append( '<' );
				sb.append( tagName );
				if ( attributeValue == null ) {
					sb.append( "/>" );
				}
				else {
					sb.append( '>' );
					//noinspection unchecked
					convertedBasicValueToString(
							sb,
							selectable.getJdbcMapping().convertToRelationalValue( attributeValue ),
							options,
							(JavaType<Object>) selectable.getJdbcMapping().getJdbcJavaType(),
							selectable.getJdbcMapping().getJdbcType()
					);
					sb.append( "</" );
					sb.append( tagName );
					sb.append( '>' );
				}
			}
			else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
				final EmbeddableMappingType mappingType = (EmbeddableMappingType) attributeMapping.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				final String tagName = aggregateMapping == null ? null : aggregateMapping.getSelectableName();
				if ( tagName != null ) {
					sb.append( '<' );
					sb.append( tagName );
					if ( attributeValue == null ) {
						sb.append( "/>" );
						continue;
					}
					sb.append( '>' );
				}
				toString(
						mappingType,
						attributeValue == null ? null : mappingType.getValues( attributeValue ),
						options,
						sb
				);
				if ( tagName != null ) {
					sb.append( "</" );
					sb.append( tagName );
					sb.append( '>' );
				}
			}
			else {
				throw new UnsupportedOperationException( "Support for attribute mapping type not yet implemented: " + attributeMapping.getClass().getName() );
			}
		}
	}

	private static void convertedBasicValueToString(
			XMLAppender appender,
			Object value,
			WrapperOptions options,
			JavaType<Object> jdbcJavaType,
			JdbcType jdbcType) {
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( value instanceof Boolean booleanValue ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( booleanValue ? '1' : '0' );
					break;
				}
				if ( value instanceof Enum<?> enumValue ) {
					appender.appendSql( enumValue.ordinal() );
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
			case SqlTypes.UUID:
				jdbcJavaType.appendEncodedString(
						appender,
						jdbcJavaType.unwrap(
								value,
								jdbcJavaType.getJavaTypeClass(),
								options
						)
				);
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( value instanceof Boolean booleanValue ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( booleanValue ? 'Y' : 'N' );
					break;
				}
			case SqlTypes.LONGVARCHAR:
			case SqlTypes.LONGNVARCHAR:
			case SqlTypes.LONG32VARCHAR:
			case SqlTypes.LONG32NVARCHAR:
			case SqlTypes.CLOB:
			case SqlTypes.MATERIALIZED_CLOB:
			case SqlTypes.NCLOB:
			case SqlTypes.MATERIALIZED_NCLOB:
			case SqlTypes.ENUM:
			case SqlTypes.NAMED_ENUM:
				appender.startEscaping();
				jdbcJavaType.appendEncodedString(
						appender,
						jdbcJavaType.unwrap(
								value,
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
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:
				appender.write( jdbcJavaType.unwrap( value, byte[].class, options ) );
				break;
			case SqlTypes.ARRAY:
			case SqlTypes.XML_ARRAY:
				final int length = Array.getLength( value );
				if ( length != 0 ) {
					//noinspection unchecked
					final JavaType<Object> elementJavaType = ( (BasicPluralJavaType<Object>) jdbcJavaType ).getElementJavaType();
					final JdbcType elementJdbcType = ( (ArrayJdbcType) jdbcType ).getElementJdbcType();

					if ( elementJdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
						final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
						for ( int i = 0; i < length; i++ ) {
							final Object arrayElement = Array.get( value, i );
							if ( arrayElement == null ) {
								appender.append( NULL_TAG );
							}
							else {
								final Object[] arrayElementValues = embeddableMappingType.getValues( arrayElement );
								appender.append( START_TAG );
								toString( embeddableMappingType, arrayElementValues, options, appender );
								appender.append( END_TAG );
							}
						}
					}
					else {
						for ( int i = 0; i < length; i++ ) {
							final Object arrayElement = Array.get( value, i );
							if ( arrayElement == null ) {
								appender.append( NULL_TAG );
							}
							else {
								appender.append( START_TAG );
								convertedBasicValueToString( appender, arrayElement, options, elementJavaType, elementJdbcType );
								appender.append( END_TAG );
							}
						}
					}
				}
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in struct: " + jdbcType );
		}
	}

	private static int getSelectableMapping(
			EmbeddableMappingType embeddableMappingType,
			String name) {
		final int selectableIndex = embeddableMappingType.getSelectableIndex( name );
		if ( selectableIndex == -1 ) {
			throw new IllegalArgumentException(
					String.format(
							"Could not find selectable [%s] in embeddable type [%s] for XML processing.",
							name,
							embeddableMappingType.getMappedJavaType().getJavaTypeClass().getName()
					)
			);
		}
		return selectableIndex;
	}

	public static boolean isValidXmlName(String name) {
		if ( name.isEmpty()
				|| !isValidXmlNameStart( name.charAt( 0 ) )
				|| name.regionMatches( true, 0, "xml", 0, 3 ) ) {
			return false;
		}
		for ( int i = 1; i < name.length(); i++ ) {
			if ( !isValidXmlNameChar( name.charAt( i ) ) ) {
				return false;
			}
		}
		return true;
	}

	public static boolean isValidXmlNameStart(char c) {
		return isLetter( c ) || c == '_' || c == ':';
	}

	public static boolean isValidXmlNameChar(char c) {
		return isLetterOrDigit( c ) || c == '_' || c == ':' || c == '-' || c == '.';
	}

	private static class XMLAppender extends OutputStream implements SqlAppender {

		private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
		private final StringBuilder sb;
		private boolean escape;

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
	}

	private static final CollectionTags DEFAULT = new CollectionTags( "Collection", ROOT_TAG );

	public static CollectionTags determineCollectionTags(BasicPluralJavaType<?> pluralJavaType, SessionFactoryImplementor sessionFactory) {
		if ( !sessionFactory.getSessionFactoryOptions().isXmlFormatMapperLegacyFormatEnabled() ) {
			return DEFAULT;
		}
		//noinspection unchecked
		final JavaType<Object> javaType = (JavaType<Object>) pluralJavaType;
		// Produce the XML string for a collection with a null element to find out the root and element tag names
		final String nullElementXml =
				sessionFactory.getSessionFactoryOptions().getXmlFormatMapper()
						.toString( javaType.fromString( "{null}" ), javaType, sessionFactory.getWrapperOptions() );

		// There must be an end tag for the root, so find that first
		final int rootCloseTagPosition = nullElementXml.lastIndexOf( '<' );
		assert nullElementXml.charAt( rootCloseTagPosition + 1 ) == '/';
		final int rootNameStart = rootCloseTagPosition + 2;
		final int rootCloseTagEnd = nullElementXml.indexOf( '>', rootCloseTagPosition );
		final String rootTag = nullElementXml.substring( rootNameStart, rootCloseTagEnd );

		// Then search for the open tag of the root and determine the start of the first item
		final int itemTagStart = nullElementXml.indexOf(
				'<',
				nullElementXml.indexOf( "<" + rootTag + ">" ) + rootTag.length() + 2
		);
		final int itemNameStart = itemTagStart + 1;
		int itemNameEnd = itemNameStart;
		for ( int i = itemNameStart + 1; i < nullElementXml.length(); i++ ) {
			if ( !isValidXmlNameChar( nullElementXml.charAt( i ) ) ) {
				itemNameEnd = i;
				break;
			}
		}
		final String elementNodeName = nullElementXml.substring( itemNameStart, itemNameEnd );
		return new CollectionTags( rootTag, elementNodeName );
	}

	public record CollectionTags(String rootName, String elementName) {}

}
