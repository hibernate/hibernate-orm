/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.type;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.hibernate.Hibernate;
import org.hibernate.MappingException;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.intercept.LazyPropertyInitializer;
import org.hibernate.property.BackrefPropertyAccessor;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.util.ReflectHelper;

/**
 * Used internally to obtain instances of <tt>Type</tt>. Applications should use static methods
 * and constants on <tt>org.hibernate.Hibernate</tt>.
 *
 * @see org.hibernate.Hibernate
 * @author Gavin King
 */
public final class TypeFactory {

	private static final Map BASIC_TYPES;

	static {
		HashMap basics = new HashMap();
		basics.put( boolean.class.getName(), Hibernate.BOOLEAN );
		basics.put( long.class.getName(), Hibernate.LONG );
		basics.put( short.class.getName(), Hibernate.SHORT );
		basics.put( int.class.getName(), Hibernate.INTEGER );
		basics.put( byte.class.getName(), Hibernate.BYTE );
		basics.put( float.class.getName(), Hibernate.FLOAT );
		basics.put( double.class.getName(), Hibernate.DOUBLE );
		basics.put( char.class.getName(), Hibernate.CHARACTER );
		basics.put( Hibernate.CHARACTER.getName(), Hibernate.CHARACTER );
		basics.put( Hibernate.INTEGER.getName(), Hibernate.INTEGER );
		basics.put( Hibernate.STRING.getName(), Hibernate.STRING );
		basics.put( Hibernate.DATE.getName(), Hibernate.DATE );
		basics.put( Hibernate.TIME.getName(), Hibernate.TIME );
		basics.put( Hibernate.TIMESTAMP.getName(), Hibernate.TIMESTAMP );
		basics.put( "dbtimestamp", new DbTimestampType() );
		basics.put( Hibernate.LOCALE.getName(), Hibernate.LOCALE );
		basics.put( Hibernate.CALENDAR.getName(), Hibernate.CALENDAR );
		basics.put( Hibernate.CALENDAR_DATE.getName(), Hibernate.CALENDAR_DATE );
		basics.put( Hibernate.CURRENCY.getName(), Hibernate.CURRENCY );
		basics.put( Hibernate.TIMEZONE.getName(), Hibernate.TIMEZONE );
		basics.put( Hibernate.CLASS.getName(), Hibernate.CLASS );
		basics.put( Hibernate.TRUE_FALSE.getName(), Hibernate.TRUE_FALSE );
		basics.put( Hibernate.YES_NO.getName(), Hibernate.YES_NO );
		basics.put( Hibernate.BINARY.getName(), Hibernate.BINARY );
		basics.put( Hibernate.IMAGE.getName(), Hibernate.IMAGE );
		basics.put( Hibernate.TEXT.getName(), Hibernate.TEXT );
		basics.put( Hibernate.MATERIALIZED_BLOB.getName(), Hibernate.MATERIALIZED_BLOB );
		basics.put( Hibernate.MATERIALIZED_CLOB.getName(), Hibernate.MATERIALIZED_CLOB );
		basics.put( Hibernate.BLOB.getName(), Hibernate.BLOB );
		basics.put( Hibernate.CLOB.getName(), Hibernate.CLOB );
		basics.put( Hibernate.BIG_DECIMAL.getName(), Hibernate.BIG_DECIMAL );
		basics.put( Hibernate.BIG_INTEGER.getName(), Hibernate.BIG_INTEGER );
		basics.put( Hibernate.SERIALIZABLE.getName(), Hibernate.SERIALIZABLE );
		basics.put( Hibernate.OBJECT.getName(), Hibernate.OBJECT );
		basics.put( Boolean.class.getName(), Hibernate.BOOLEAN );
		basics.put( Long.class.getName(), Hibernate.LONG );
		basics.put( Short.class.getName(), Hibernate.SHORT );
		basics.put( Integer.class.getName(), Hibernate.INTEGER );
		basics.put( Byte.class.getName(), Hibernate.BYTE );
		basics.put( Float.class.getName(), Hibernate.FLOAT );
		basics.put( Double.class.getName(), Hibernate.DOUBLE );
		basics.put( Character.class.getName(), Hibernate.CHARACTER );
		basics.put( String.class.getName(), Hibernate.STRING );
		basics.put( java.util.Date.class.getName(), Hibernate.TIMESTAMP );
		basics.put( Time.class.getName(), Hibernate.TIME );
		basics.put( Timestamp.class.getName(), Hibernate.TIMESTAMP );
		basics.put( java.sql.Date.class.getName(), Hibernate.DATE );
		basics.put( BigDecimal.class.getName(), Hibernate.BIG_DECIMAL );
		basics.put( BigInteger.class.getName(), Hibernate.BIG_INTEGER );
		basics.put( Locale.class.getName(), Hibernate.LOCALE );
		basics.put( Calendar.class.getName(), Hibernate.CALENDAR );
		basics.put( GregorianCalendar.class.getName(), Hibernate.CALENDAR );
		if ( CurrencyType.CURRENCY_CLASS != null ) {
			basics.put( CurrencyType.CURRENCY_CLASS.getName(), Hibernate.CURRENCY );
		}
		basics.put( TimeZone.class.getName(), Hibernate.TIMEZONE );
		basics.put( Object.class.getName(), Hibernate.OBJECT );
		basics.put( Class.class.getName(), Hibernate.CLASS );
		basics.put( byte[].class.getName(), Hibernate.BINARY );
		basics.put( "byte[]", Hibernate.BINARY );
		basics.put( Byte[].class.getName(), Hibernate.WRAPPER_BINARY );
		basics.put( "Byte[]", Hibernate.WRAPPER_BINARY );
		basics.put( char[].class.getName(), Hibernate.CHAR_ARRAY );
		basics.put( "char[]", Hibernate.CHAR_ARRAY );
		basics.put( Character[].class.getName(), Hibernate.CHARACTER_ARRAY );
		basics.put( "Character[]", Hibernate.CHARACTER_ARRAY );
		basics.put( Blob.class.getName(), Hibernate.BLOB );
		basics.put( Clob.class.getName(), Hibernate.CLOB );
		basics.put( Serializable.class.getName(), Hibernate.SERIALIZABLE );

		Type type = new AdaptedImmutableType(Hibernate.DATE);
		basics.put( type.getName(), type );
		type = new AdaptedImmutableType(Hibernate.TIME);
		basics.put( type.getName(), type );
		type = new AdaptedImmutableType(Hibernate.TIMESTAMP);
		basics.put( type.getName(), type );
		type = new AdaptedImmutableType( new DbTimestampType() );
		basics.put( type.getName(), type );
		type = new AdaptedImmutableType(Hibernate.CALENDAR);
		basics.put( type.getName(), type );
		type = new AdaptedImmutableType(Hibernate.CALENDAR_DATE);
		basics.put( type.getName(), type );
		type = new AdaptedImmutableType(Hibernate.SERIALIZABLE);
		basics.put( type.getName(), type );
		type = new AdaptedImmutableType(Hibernate.BINARY);
		basics.put( type.getName(), type );

		BASIC_TYPES = Collections.unmodifiableMap( basics );
	}

	private TypeFactory() {
		throw new UnsupportedOperationException();
	}

	/**
	 * A one-to-one association type for the given class
	 */
	public static EntityType oneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			String entityName,
			String propertyName
	) {
		return new OneToOneType(
				persistentClass,
				foreignKeyType,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				isEmbeddedInXML,
				entityName,
				propertyName
			);
	}

	/**
	 * A many-to-one association type for the given class
	 */
	public static EntityType manyToOne(String persistentClass) {
		return new ManyToOneType( persistentClass );
	}

	/**
	 * A many-to-one association type for the given class
	 */
	public static EntityType manyToOne(String persistentClass, boolean lazy) {
		return new ManyToOneType( persistentClass, lazy );
	}

	/**
	 * A many-to-one association type for the given class
	 *
	 * @deprecated Use {@link #manyToOne(String, String, boolean, boolean, boolean, boolean, boolean)}
	 */
	public static EntityType manyToOne(
			String persistentClass,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			boolean ignoreNotFound) {
		//noinspection deprecation
		return new ManyToOneType(
				persistentClass,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				isEmbeddedInXML,
				ignoreNotFound
		);
	}

	/**
	 * A many-to-one association type for the given class
	 */
	public static EntityType manyToOne(
			String persistentClass,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return new ManyToOneType(
				persistentClass,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				isEmbeddedInXML,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	/**
	 * Given the name of a Hibernate basic type, return an instance of
	 * <tt>org.hibernate.type.Type</tt>.
	 */
	public static Type basic(String name) {
		return (Type) BASIC_TYPES.get( name );
	}

	/**
	 * Uses heuristics to deduce a Hibernate type given a string naming the type or Java class.
	 * Return an instance of <tt>org.hibernate.type.Type</tt>.
	 */
	public static Type heuristicType(String typeName) throws MappingException {
		return heuristicType( typeName, null );
	}

	/**
	 * Uses heuristics to deduce a Hibernate type given a string naming the type or Java class.
	 * Return an instance of <tt>org.hibernate.type.Type</tt>.
	 */
	public static Type heuristicType(String typeName, Properties parameters)
			throws MappingException {
		Type type = TypeFactory.basic( typeName );
		if ( type == null ) {
			Class typeClass;
			try {
				typeClass = ReflectHelper.classForName( typeName );
			}
			catch (ClassNotFoundException cnfe) {
				typeClass = null;
			}
			if ( typeClass != null ) {
				if ( Type.class.isAssignableFrom( typeClass ) ) {
					try {
						type = (Type) typeClass.newInstance();
					}
					catch (Exception e) {
						throw new MappingException(
								"Could not instantiate Type: " + typeClass.getName(),
								e
							);
					}
					injectParameters(type, parameters);
				}
				else if ( CompositeUserType.class.isAssignableFrom( typeClass ) ) {
					type = new CompositeCustomType( typeClass, parameters );
				}
				else if ( UserType.class.isAssignableFrom( typeClass ) ) {
					type = new CustomType( typeClass, parameters );
				}
				else if ( Lifecycle.class.isAssignableFrom( typeClass ) ) {
					type = Hibernate.entity( typeClass );
				}
				else if ( Serializable.class.isAssignableFrom( typeClass ) ) {
					type = Hibernate.serializable( typeClass );
				}
			}
		}
		return type;

	}

	/**
	 * The legacy contract.
	 *
	 * @deprecated Use {@link #customCollection(String, java.util.Properties, String, String, boolean)} instead
	 */
	public static CollectionType customCollection(
			String typeName,
			String role,
			String propertyRef,
			boolean embedded) {
		return customCollection( typeName, null, role, propertyRef, embedded );
	}

	public static CollectionType customCollection(
			String typeName,
			Properties typeParameters,
			String role,
			String propertyRef,
			boolean embedded) {
		Class typeClass;
		try {
			typeClass = ReflectHelper.classForName( typeName );
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new MappingException( "user collection type class not found: " + typeName, cnfe );
		}
		CustomCollectionType result = new CustomCollectionType( typeClass, role, propertyRef, embedded );
		if ( typeParameters != null ) {
			TypeFactory.injectParameters( result.getUserType(), typeParameters );
		}
		return result;
	}

	// Collection Types:

	public static CollectionType array(String role, String propertyRef, boolean embedded,
			Class elementClass) {
		return new ArrayType( role, propertyRef, elementClass, embedded );
	}

	public static CollectionType list(String role, String propertyRef, boolean embedded) {
		return new ListType( role, propertyRef, embedded );
	}

	public static CollectionType bag(String role, String propertyRef, boolean embedded) {
		return new BagType( role, propertyRef, embedded );
	}

	public static CollectionType idbag(String role, String propertyRef, boolean embedded) {
		return new IdentifierBagType( role, propertyRef, embedded );
	}

	public static CollectionType map(String role, String propertyRef, boolean embedded) {
		return new MapType( role, propertyRef, embedded );
	}

	public static CollectionType orderedMap(String role, String propertyRef, boolean embedded) {
		return new OrderedMapType( role, propertyRef, embedded );
	}

	public static CollectionType set(String role, String propertyRef, boolean embedded) {
		return new SetType( role, propertyRef, embedded );
	}

	public static CollectionType orderedSet(String role, String propertyRef, boolean embedded) {
		return new OrderedSetType( role, propertyRef, embedded );
	}

	public static CollectionType sortedMap(String role, String propertyRef, boolean embedded,
			Comparator comparator) {
		return new SortedMapType( role, propertyRef, comparator, embedded );
	}

	public static CollectionType sortedSet(String role, String propertyRef, boolean embedded,
			Comparator comparator) {
		return new SortedSetType( role, propertyRef, comparator, embedded );
	}

	public static void injectParameters(Object type, Properties parameters) {
		if (type instanceof ParameterizedType) {
			( (ParameterizedType) type ).setParameterValues(parameters);
		}
		else if ( parameters!=null && !parameters.isEmpty() ) {
			throw new MappingException(
					"type is not parameterized: " +
					type.getClass().getName()
				);
		}
	}


	// convenience methods relating to operations across arrays of types...

	/**
	 * Deep copy a series of values from one array to another...
	 *
	 * @param values The values to copy (the source)
	 * @param types The value types
	 * @param copy an array indicating which values to include in the copy
	 * @param target The array into which to copy the values
	 * @param session The orginating session
	 */
	public static void deepCopy(
			final Object[] values,
			final Type[] types,
			final boolean[] copy,
			final Object[] target,
			final SessionImplementor session) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( copy[i] ) {
				if ( values[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| values[i] == BackrefPropertyAccessor.UNKNOWN ) {
					target[i] = values[i];
				}
				else {
					target[i] = types[i].deepCopy( values[i], session.getEntityMode(), session
						.getFactory() );
				}
			}
		}
	}

	/**
	 * Apply the {@link Type#beforeAssemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param session The orginating session
	 */
	public static void beforeAssemble(
			final Serializable[] row,
			final Type[] types,
			final SessionImplementor session) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( row[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY
				&& row[i] != BackrefPropertyAccessor.UNKNOWN ) {
				types[i].beforeAssemble( row[i], session );
			}
		}
	}

	/**
	 * Apply the {@link Type#assemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param session The orginating session
	 * @param owner The entity "owning" the values
	 * @return The assembled state
	 */
	public static Object[] assemble(
			final Serializable[] row,
			final Type[] types,
			final SessionImplementor session,
			final Object owner) {
		Object[] assembled = new Object[row.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY || row[i] == BackrefPropertyAccessor.UNKNOWN ) {
				assembled[i] = row[i];
			}
			else {
				assembled[i] = types[i].assemble( row[i], session, owner );
			}
		}
		return assembled;
	}

	/**
	 * Apply the {@link Type#disassemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param nonCacheable An array indicating which values to include in the disassemled state
	 * @param session The orginating session
	 * @param owner The entity "owning" the values
	 * @return The disassembled state
	 */
	public static Serializable[] disassemble(
			final Object[] row,
			final Type[] types,
			final boolean[] nonCacheable,
			final SessionImplementor session,
			final Object owner) {
		Serializable[] disassembled = new Serializable[row.length];
		for ( int i = 0; i < row.length; i++ ) {
			if ( nonCacheable!=null && nonCacheable[i] ) {
				disassembled[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
			else if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY || row[i] == BackrefPropertyAccessor.UNKNOWN ) {
				disassembled[i] = (Serializable) row[i];
			}
			else {
				disassembled[i] = types[i].disassemble( row[i], session, owner );
			}
		}
		return disassembled;
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The orginating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 * @return The replaced state
	 */
	public static Object[] replace(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SessionImplementor session,
			final Object owner,
			final Map copyCache) {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| original[i] == BackrefPropertyAccessor.UNKNOWN ) {
				copied[i] = target[i];
			}
			else {
				copied[i] = types[i].replace( original[i], target[i], session, owner, copyCache );
			}
		}
		return copied;
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The orginating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 * @param foreignKeyDirection FK directionality to be applied to the replacement
	 * @return The replaced state
	 */
	public static Object[] replace(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SessionImplementor session,
			final Object owner,
			final Map copyCache,
			final ForeignKeyDirection foreignKeyDirection) {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| original[i] == BackrefPropertyAccessor.UNKNOWN ) {
				copied[i] = target[i];
			}
			else {
				copied[i] = types[i].replace( original[i], target[i], session, owner, copyCache, foreignKeyDirection );
			}
		}
		return copied;
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values, as
	 * long as the corresponding {@link Type} is an association.
	 * <p/>
	 * If the corresponding type is a component type, then apply {@link #replaceAssociations}
	 * accross the component subtypes but do not replace the component value itself.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The orginating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 * @param foreignKeyDirection FK directionality to be applied to the replacement
	 * @return The replaced state
	 */
	public static Object[] replaceAssociations(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SessionImplementor session,
			final Object owner,
			final Map copyCache,
			final ForeignKeyDirection foreignKeyDirection) {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| original[i] == BackrefPropertyAccessor.UNKNOWN ) {
				copied[i] = target[i];
			}
			else if ( types[i].isComponentType() ) {
				// need to extract the component values and check for subtype replacements...
				AbstractComponentType componentType = ( AbstractComponentType ) types[i];
				Type[] subtypes = componentType.getSubtypes();
				Object[] origComponentValues = original[i] == null ? new Object[subtypes.length] : componentType.getPropertyValues( original[i], session );
				Object[] targetComponentValues = target[i] == null ? new Object[subtypes.length] : componentType.getPropertyValues( target[i], session );
				replaceAssociations( origComponentValues, targetComponentValues, subtypes, session, null, copyCache, foreignKeyDirection );
				copied[i] = target[i];
			}
			else if ( !types[i].isAssociationType() ) {
				copied[i] = target[i];
			}
			else {
				copied[i] = types[i].replace( original[i], target[i], session, owner, copyCache, foreignKeyDirection );
			}
		}
		return copied;
	}

	/**
	 * Determine if any of the given field values are dirty, returning an array containing
	 * indices of the dirty fields.
	 * <p/>
	 * If it is determined that no fields are dirty, null is returned.
	 *
	 * @param properties The property definitions
	 * @param currentState The current state of the entity
	 * @param previousState The baseline state of the entity
	 * @param includeColumns Columns to be included in the dirty checking, per property
	 * @param anyUninitializedProperties Does the entity currently hold any uninitialized property values?
	 * @param session The session from which the dirty check request originated.
	 * @return Array containing indices of the dirty properties, or null if no properties considered dirty.
	 */
	public static int[] findDirty(
			final StandardProperty[] properties,
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean anyUninitializedProperties,
			final SessionImplementor session) {
		int[] results = null;
		int count = 0;
		int span = properties.length;

		for ( int i = 0; i < span; i++ ) {
			final boolean dirty = currentState[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& properties[i].isDirtyCheckable( anyUninitializedProperties )
					&& properties[i].getType().isDirty( previousState[i], currentState[i], includeColumns[i], session );
			if ( dirty ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}
		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}

	/**
	 * Determine if any of the given field values are modified, returning an array containing
	 * indices of the modified fields.
	 * <p/>
	 * If it is determined that no fields are dirty, null is returned.
	 *
	 * @param properties The property definitions
	 * @param currentState The current state of the entity
	 * @param previousState The baseline state of the entity
	 * @param includeColumns Columns to be included in the mod checking, per property
	 * @param anyUninitializedProperties Does the entity currently hold any uninitialized property values?
	 * @param session The session from which the dirty check request originated.
	 * @return Array containing indices of the modified properties, or null if no properties considered modified.
	 */
	public static int[] findModified(
			final StandardProperty[] properties, 
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean anyUninitializedProperties,
			final SessionImplementor session) {
		int[] results = null;
		int count = 0;
		int span = properties.length;

		for ( int i = 0; i < span; i++ ) {
			final boolean modified = currentState[i]!=LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& properties[i].isDirtyCheckable(anyUninitializedProperties)
					&& properties[i].getType().isModified( previousState[i], currentState[i], includeColumns[i], session );

			if ( modified ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}
		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}

}
