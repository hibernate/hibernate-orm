/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

/**
 * A registry of {@link BasicType} instances
 *
 * @author Steve Ebersole
 */
public class BasicTypeRegistry implements Serializable {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, BasicTypeRegistry.class.getName());

	// TODO : analyze these sizing params; unfortunately this seems to be the only way to give a "concurrencyLevel"
	private Map<String,BasicType> registry = new ConcurrentHashMap<String, BasicType>( 100, .75f, 1 );
	private boolean locked = false;

	public BasicTypeRegistry() {
		register( BooleanType.INSTANCE );
		register( NumericBooleanType.INSTANCE );
		register( TrueFalseType.INSTANCE );
		register( YesNoType.INSTANCE );

		register( ByteType.INSTANCE );
		register( CharacterType.INSTANCE );
		register( ShortType.INSTANCE );
		register( IntegerType.INSTANCE );
		register( LongType.INSTANCE );
		register( FloatType.INSTANCE );
		register( DoubleType.INSTANCE );
		register( BigDecimalType.INSTANCE );
		register( BigIntegerType.INSTANCE );

		register( StringType.INSTANCE );
		register( UrlType.INSTANCE );

		register( DateType.INSTANCE );
		register( TimeType.INSTANCE );
		register( TimestampType.INSTANCE );
		register( DbTimestampType.INSTANCE );
		register( CalendarType.INSTANCE );
		register( CalendarDateType.INSTANCE );

		register( LocaleType.INSTANCE );
		register( CurrencyType.INSTANCE );
		register( TimeZoneType.INSTANCE );
		register( ClassType.INSTANCE );
		register( UUIDBinaryType.INSTANCE );
		register( UUIDCharType.INSTANCE );
		register( PostgresUUIDType.INSTANCE );

		register( BinaryType.INSTANCE );
		register( WrapperBinaryType.INSTANCE );
		register( ImageType.INSTANCE );
		register( CharArrayType.INSTANCE );
		register( CharacterArrayType.INSTANCE );
		register( TextType.INSTANCE );
		register( BlobType.INSTANCE );
		register( MaterializedBlobType.INSTANCE );
		register( ClobType.INSTANCE );
		register( MaterializedClobType.INSTANCE );
		register( SerializableType.INSTANCE );

		register( ObjectType.INSTANCE );

		//noinspection unchecked
		register( new AdaptedImmutableType( DateType.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableType( TimeType.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableType( TimestampType.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableType( DbTimestampType.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableType( CalendarType.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableType( CalendarDateType.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableType( BinaryType.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableType( SerializableType.INSTANCE ) );
	}

	/**
	 * Constructor version used during shallow copy
	 *
	 * @param registeredTypes The type map to copy over
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	private BasicTypeRegistry(Map<String, BasicType> registeredTypes) {
		registry.putAll( registeredTypes );
		locked = true;
	}

	public void register(BasicType type) {
		if ( locked ) {
			throw new HibernateException( "Can not alter TypeRegistry at this time" );
		}

		if ( type == null ) {
			throw new HibernateException( "Type to register cannot be null" );
		}

		if ( type.getRegistrationKeys() == null || type.getRegistrationKeys().length == 0 ) {
			LOG.typeDefinedNoRegistrationKeys( type );
		}

		for ( String key : type.getRegistrationKeys() ) {
			// be safe...
            if (key == null) continue;
            LOG.debugf("Adding type registration %s -> %s", key, type);
			final Type old = registry.put( key, type );
            if (old != null && old != type) LOG.typeRegistrationOverridesPrevious(key, old);
		}
	}

	public void register(UserType type, String[] keys) {
		register( new CustomType( type, keys ) );
	}

	public void register(CompositeUserType type, String[] keys) {
		register( new CompositeCustomType( type, keys ) );
	}

	public BasicType getRegisteredType(String key) {
		return registry.get( key );
	}

	public BasicTypeRegistry shallowCopy() {
		return new BasicTypeRegistry( this.registry );
	}
}
