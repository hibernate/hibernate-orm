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
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.internal.StandardBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

/**
 * A registry of {@link BasicType} instances
 *
 * @author Steve Ebersole
 */
public class BasicTypeRegistry implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BasicTypeRegistry.class );

	private final TypeConfiguration typeConfiguration;

	private final Map<SqlTypeDescriptor,Map<JavaTypeDescriptor<?>,BasicType<?>>> registryValues = new ConcurrentHashMap<>();

	/**
	 * TODO : analyze these sizing params; unfortunately this seems to be the only way to give a "concurrencyLevel"
	 */
	private Map<String, BasicType> typesByName = new ConcurrentHashMap<>( 100, .75f, 1 );

	public BasicTypeRegistry(TypeConfiguration typeConfiguration){
		this.typeConfiguration = typeConfiguration;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access

	public BasicType getRegisteredType(String key) {
		return typesByName.get( key );
	}

	public BasicType getRegisteredType(Class javaType) {
		return getRegisteredType( javaType.getName() );
	}

	/**
	 * Find an existing BasicType registration for the given JavaTypeDescriptor and
	 * SqlTypeDescriptor combo or create (and register) one.
	 */
	public BasicType<?> resolve(JavaTypeDescriptor<?> jtdToUse, SqlTypeDescriptor stdToUse) {
		//noinspection unchecked
		return resolve( jtdToUse, stdToUse, () -> new StandardBasicTypeImpl( jtdToUse, stdToUse ) );
	}

	/**
	 * Find an existing BasicType registration for the given JavaTypeDescriptor and
	 * SqlTypeDescriptor combo or create (and register) one.
	 */
	public BasicType<?> resolve(JavaTypeDescriptor<?> jtdToUse, SqlTypeDescriptor stdToUse, Supplier<BasicType<?>> creator) {
		final Map<JavaTypeDescriptor<?>, BasicType<?>> typeByJtdForStd = registryValues.computeIfAbsent(
				stdToUse,
				sqlTypeDescriptor -> new ConcurrentHashMap<>()
		);

		return typeByJtdForStd.computeIfAbsent( jtdToUse, javaDescriptor -> creator.get() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutations

	public void register(BasicType type) {
		register( type, type.getRegistrationKeys() );
	}

	public void register(BasicType type, String key) {
		typesByName.put( key, type );
	}

	public void register(BasicType type, String... keys) {
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

			final Type old = typesByName.put( key, type );
			if ( old != null && old != type ) {
				LOG.typeRegistrationOverridesPrevious( key, old );
			}

			final Map<JavaTypeDescriptor<?>, BasicType<?>> mappingsForStdToUse = registryValues.computeIfAbsent(
					type.getSqlTypeDescriptor(),
					sqlTypeDescriptor -> new ConcurrentHashMap<>()
			);

			//noinspection unchecked
			mappingsForStdToUse.computeIfAbsent(
					type.getMappedJavaTypeDescriptor(),
					javaDescriptor -> new StandardBasicTypeImpl(
							javaDescriptor,
							type.getSqlTypeDescriptor()
					)
			);

		}
	}

	public void register(UserType type, String... keys) {
		register( new CustomType( type, keys, typeConfiguration ) );
	}

	public void unregister(String... keys) {
		for ( String key : keys ) {
			final BasicType removed = typesByName.remove( key );


		}
	}
}
