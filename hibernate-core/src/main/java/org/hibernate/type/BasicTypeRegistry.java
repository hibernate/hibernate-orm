/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

/**
 * A registry of {@link BasicType} instances
 *
 * @author Steve Ebersole
 */
public class BasicTypeRegistry implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BasicTypeRegistry.class );

	// TODO : analyze these sizing params; unfortunately this seems to be the only way to give a "concurrencyLevel"
	private Map<String, BasicType> registry = new ConcurrentHashMap<>( 100, .75f, 1 );
	private boolean locked;
	private TypeConfiguration typeConfiguration;

	public BasicTypeRegistry(TypeConfiguration typeConfiguration){
		this();
		this.typeConfiguration = typeConfiguration;
	}

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
		register( StringNVarcharType.INSTANCE );
		register( CharacterNCharType.INSTANCE );
		register( UrlType.INSTANCE );

		register( DurationType.INSTANCE );
		register( InstantType.INSTANCE );
		register( LocalDateTimeType.INSTANCE );
		register( LocalDateType.INSTANCE );
		register( LocalTimeType.INSTANCE );
		register( OffsetDateTimeType.INSTANCE );
		register( OffsetTimeType.INSTANCE );
		register( ZonedDateTimeType.INSTANCE );

		register( DateType.INSTANCE );
		register( TimeType.INSTANCE );
		register( TimestampType.INSTANCE );
		register( DbTimestampType.INSTANCE );
		register( CalendarType.INSTANCE );
		register( CalendarDateType.INSTANCE );
		register( CalendarTimeType.INSTANCE );

		register( LocaleType.INSTANCE );
		register( CurrencyType.INSTANCE );
		register( TimeZoneType.INSTANCE );
		register( ClassType.INSTANCE );
		register( UUIDBinaryType.INSTANCE );
		register( UUIDCharType.INSTANCE );

		register( BinaryType.INSTANCE );
		register( WrapperBinaryType.INSTANCE );
		register( RowVersionType.INSTANCE );
		register( ImageType.INSTANCE );
		register( CharArrayType.INSTANCE );
		register( CharacterArrayType.INSTANCE );
		register( TextType.INSTANCE );
		register( NTextType.INSTANCE );
		register( BlobType.INSTANCE );
		register( MaterializedBlobType.INSTANCE );
		register( ClobType.INSTANCE );
		register( NClobType.INSTANCE );
		register( MaterializedClobType.INSTANCE );
		register( MaterializedNClobType.INSTANCE );
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
	@SuppressWarnings({"UnusedDeclaration"})
	private BasicTypeRegistry(Map<String, BasicType> registeredTypes) {
		registry.putAll( registeredTypes );
		locked = true;
	}

	public void register(BasicType type) {
		register( type, type.getRegistrationKeys() );
	}

	public void register(BasicType type, String[] keys) {
		if ( locked ) {
			throw new HibernateException( "Can not alter TypeRegistry at this time" );
		}

		if ( type == null ) {
			throw new HibernateException( "Type to register cannot be null" );
		}

		if ( keys == null || keys.length == 0 ) {
			LOG.typeDefinedNoRegistrationKeys( type );
			return;
		}

		for ( String key : keys ) {
			// be safe...
			if ( key == null ) {
				continue;
			}
			//Use String#intern here as there's high chances of duplicates combined with long term usage:
			//just running our testsuite would generate 210,000 instances for the String "java.lang.Class" alone.
			//Incidentally this might help with map lookup efficiency too.
			key = key.intern();
			LOG.debugf( "Adding type registration %s -> %s", key, type );
			final Type old = registry.put( key, type );
			if ( old != null && old != type ) {
				LOG.typeRegistrationOverridesPrevious( key, old );
			}
		}
	}

	public void register(UserType type, String[] keys) {
		register( new CustomType( type, keys ) );
	}

	public void register(CompositeUserType type, String[] keys) {
		register( new CompositeCustomType( type, keys ) );
	}

	public void unregister(String... keys) {
		for ( String key : keys ) {
			registry.remove( key );
		}
	}

	public BasicType getRegisteredType(String key) {
		return registry.get( key );
	}

	public BasicTypeRegistry shallowCopy() {
		return new BasicTypeRegistry( this.registry );
	}
}
