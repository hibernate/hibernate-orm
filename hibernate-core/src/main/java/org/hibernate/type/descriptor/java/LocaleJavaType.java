/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.Comparator;
import java.util.Locale;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Locale} handling.
 *
 * @author Steve Ebersole
 */
public class LocaleJavaType extends AbstractClassJavaType<Locale> {
	public static final LocaleJavaType INSTANCE = new LocaleJavaType();

	public static class LocaleComparator implements Comparator<Locale> {
		public static final LocaleComparator INSTANCE = new LocaleComparator();

		public int compare(Locale o1, Locale o2) {
			return o1.toString().compareTo( o2.toString() );
		}
	}

	public LocaleJavaType() {
		super( Locale.class, ImmutableMutabilityPlan.instance(), LocaleComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Locale;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	public String toString(Locale value) {
		return value.toString();
	}

	public Locale fromString(CharSequence sequence) {
		if ( sequence == null ) {
			return null;
		}

		String string = sequence.toString();
		if ( string.isEmpty() ) {
			return Locale.ROOT;
		}

		final char[] chars = string.toCharArray();
		final Locale.Builder builder = new Locale.Builder();
		State state = State.LANGUAGE;
		int position = 0;

		for ( int i = 0; i < chars.length; i++ ) {
			// We just look for separators
			if ( chars[i] == '_' ) {
				state = state.parseNext( chars, position, i - position, builder );
				position = i + 1;
			}
			// If we find a dash instead of an underscore, assume this is a BCP 47 language tag
			else if ( state == State.LANGUAGE && chars[i] == '-' ) {
				builder.setLanguageTag( string );
				position = chars.length;
				break;
			}
		}

		if ( position != chars.length ) {
			if ( state == State.LANGUAGE ) {
				// This is special, we have no separators and at least 1 character, so assume it is a language tag.
				// To retain backwards compatibility, convert old locale languages to the proper language tags.
				// All the other locale languages are equivalent to the language tag languages.
				builder.setLanguageTag( switch ( string ) {
					case "iw", "Iw", "iW", "IW" -> "he";
					case "ji", "Ji", "jI", "JI" -> "yi";
					case "in", "In", "iN", "IN" -> "id";
					default -> string;
				} );
			}
			else {
				state.parseNext( chars, position, chars.length - position, builder );
			}
		}
		return builder.build();
	}

	enum State {
		LANGUAGE,
		REGION,
		VARIANT,
		SCRIPT,
		EXTENSION,
		END;

		State parseNext(char[] chars, int start, int length, Locale.Builder builder) {
			return switch ( this ) {
				case LANGUAGE -> {
					builder.setLanguage( new String( chars, start, length ) );
					yield REGION;
				}
				case REGION -> {
					builder.setRegion( new String( chars, start, length ) );
					yield VARIANT;
				}
				case VARIANT -> {
					if ( chars[start] == '#' ) {
						if ( isScript( chars, start + 1, length - 1 ) ) {
							builder.setScript( new String( chars, start + 1, length - 1 ) );
							yield EXTENSION;
						}
						else {
							handleExtension( chars, start + 1, length - 1, builder );
							yield END;
						}
					}
					else {
						builder.setVariant( new String( chars, start, length ) );
						yield SCRIPT;
					}
				}
				case SCRIPT -> {
					if ( length < 5 || chars[start] != '#' ) {
						throw new IllegalArgumentException( "Invalid script: " + new String( chars, start, length ) );
					}
					if ( isScript( chars, start + 1, length - 1 ) ) {
						builder.setScript( new String( chars, start + 1, length - 1 ) );
						yield EXTENSION;
					}
					else {
						handleExtension( chars, start + 1, length - 1, builder );
						yield END;
					}
				}
				case EXTENSION -> {
					handleExtension(  chars, start, length, builder );
					yield END;
				}
				case END -> throw new IllegalStateException( "Unexpected continuation of locale value after extension: " + new String( chars, start, length ) );
			};
		}

		private boolean isScript(char[] chars, int start, int length) {
			return length == 4
				&& Character.isLetter( chars[start] )
				&& Character.isLetter( chars[start + 1] )
				&& Character.isLetter( chars[start + 2] )
				&& Character.isLetter( chars[start + 3] );
		}

		private void handleExtension(char[] chars, int start, int length, Locale.Builder builder) {
			if ( length < 3 || chars[start + 1] != '-' ) {
				throw new IllegalArgumentException( "Invalid extension: " + new String( chars, start, length ) );
			}
			if ( Character.toLowerCase( chars[start] ) == 'u' ) {
				// After a Unicode extension, there could come a private use extension which we need to detect
				int unicodeStart = start + 2;
				int unicodeLength = length - 2;
				final int end = start + length;
				for ( int i = start + 2; i < end; i++ ) {
					if ( chars[i] == '-' && i + 3 < end && chars[i + 1] == 'x' && chars[i + 2] == '-' ) {
						builder.setExtension( 'x', new String( chars, i + 3, end - i - 3 ) );
						unicodeLength = i - unicodeStart;
						break;
					}
				}
				builder.setExtension( chars[start], new String( chars, unicodeStart, unicodeLength ) );
			}
			else {
				builder.setExtension( chars[start], new String( chars, start + 2, length - 2 ) );
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(Locale value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Locale.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	public <X> Locale wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Locale locale ) {
			return locale;
		}
		if (value instanceof CharSequence charSequence) {
			return fromString( charSequence );
		}
		throw unknownWrap( value.getClass() );
	}

}
