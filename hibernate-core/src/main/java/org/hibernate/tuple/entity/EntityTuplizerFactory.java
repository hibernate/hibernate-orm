/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;

import org.jboss.logging.Logger;

/**
 * A registry allowing users to define the default {@link EntityTuplizer} class to use per {@link EntityMode}.
 *
 * @author Steve Ebersole
 */
public class EntityTuplizerFactory implements Serializable {
	private static final Logger log = Logger.getLogger( EntityTuplizerFactory.class );

	public static final Class[] ENTITY_TUP_CTOR_SIG = new Class[] { EntityPersister.class, EntityMapping.class };

	private Map<EntityMode,Class<? extends EntityTuplizer>> defaultImplClassByMode = buildBaseMapping();

	public EntityTuplizerFactory(BootstrapContext bootstrapContext) {

	}

	/**
	 * Method allowing registration of the tuplizer class to use as default for a particular entity-mode.
	 *
	 * @param entityMode The entity-mode for which to register the tuplizer class
	 * @param tuplizerClass The class to use as the default tuplizer for the given entity-mode.
	 */
	public void registerDefaultTuplizerClass(EntityMode entityMode, Class<? extends EntityTuplizer> tuplizerClass) {
		if ( ! isEntityTuplizerImplementor( tuplizerClass ) ) {
			throw new HibernateException(
					"Specified tuplizer class [" + tuplizerClass.getName() + "] does not implement " + EntityTuplizer.class.getName()
			);
		}

		if ( ! hasProperConstructor( tuplizerClass ) ) {
			throw new HibernateException( "Specified tuplizer class [" + tuplizerClass.getName() + "] is not properly instantiatable" );
		}

		final Class<? extends EntityTuplizer> existing = defaultImplClassByMode.put( entityMode, tuplizerClass );
		log.debugf(
				"Registered default EntityTuplizer [%s] for EntityMode [%s]; replaced [%s]",
				tuplizerClass.getName(),
				entityMode,
				existing.getName()
		);
	}

	public EntityTuplizer createTuplizer(
			String explicitTuplizerClassName,
			EntityMode entityMode,
			EntityPersister entityPersister,
			EntityMapping entityMapping,
			PersisterCreationContext persisterCreationContext) {
		final Class<? extends EntityTuplizer> explicitTuplizerClass;

		if ( StringHelper.isEmpty( explicitTuplizerClassName ) ) {
			explicitTuplizerClass = null;
		}
		else {
			explicitTuplizerClass = locateTuplizerClass( explicitTuplizerClassName, persisterCreationContext );
		}

		return createTuplizer( explicitTuplizerClass, entityMode, entityPersister, entityMapping, persisterCreationContext );
	}

	private Class<? extends EntityTuplizer> locateTuplizerClass(String tuplizerClassName, PersisterCreationContext persisterCreationContext) {
		try {
			final ClassLoaderService cls = persisterCreationContext.getSessionFactory().getServiceRegistry().getService( ClassLoaderService.class );
			return cls.classForName( tuplizerClassName );
		}
		catch (ClassLoadingException e) {
			throw new HibernateException( "Could not locate named tuplizer class [" + tuplizerClassName + "]" );
		}
	}

	public EntityTuplizer createTuplizer(
			Class<? extends EntityTuplizer> explicitTuplizerClass,
			EntityMode entityMode,
			EntityPersister entityPersister,
			EntityMapping entityMapping,
			PersisterCreationContext persisterCreationContext) {
		final Class<? extends EntityTuplizer> tuplizerClass;
		if ( explicitTuplizerClass == null ) {
			tuplizerClass = locateDefaultTuplizerClass( entityMode, persisterCreationContext );
		}
		else {
			tuplizerClass = explicitTuplizerClass;
		}

		return constructTuplizer( tuplizerClass, entityPersister, entityMapping, persisterCreationContext );
	}

	private Class<? extends EntityTuplizer> locateDefaultTuplizerClass(
			EntityMode entityMode,
			PersisterCreationContext persisterCreationContext) {
		final Class<? extends EntityTuplizer> tuplizerClass = defaultImplClassByMode.get( entityMode );

		if ( tuplizerClass == null ) {
			throw new HibernateException( "Could not locate default EntityTuplizer for given EntityMode: " + entityMode );
		}

		return tuplizerClass;
	}

	private EntityTuplizer constructTuplizer(
			Class<? extends EntityTuplizer> tuplizerClass,
			EntityPersister entityPersister,
			EntityMapping entityMapping,
			PersisterCreationContext persisterCreationContext) {
		final Constructor<? extends EntityTuplizer> constructor = getProperConstructor( tuplizerClass );
		if ( constructor == null ) {
			throw new HibernateException( "Unable to locate proper constructor for tuplizer [" + tuplizerClass.getName() + "]" );
		}

		try {
			return constructor.newInstance( entityPersister, entityMapping );
		}
		catch (Throwable t) {
			throw new HibernateException( "Unable to instantiate default tuplizer [" + tuplizerClass.getName() + "]", t );
		}
	}

	private boolean isEntityTuplizerImplementor(Class tuplizerClass) {
		return ReflectHelper.implementsInterface( tuplizerClass, EntityTuplizer.class );
	}

	private boolean hasProperConstructor(Class<? extends EntityTuplizer> tuplizerClass) {
		return getProperConstructor( tuplizerClass ) != null
				&& ! ReflectHelper.isAbstractClass( tuplizerClass );
	}

	private Constructor<? extends EntityTuplizer> getProperConstructor(Class<? extends EntityTuplizer> clazz) {
		Constructor<? extends EntityTuplizer> constructor = null;
		try {
			constructor = clazz.getDeclaredConstructor( ENTITY_TUP_CTOR_SIG );
			try {
				constructor.setAccessible( true );
			}
			catch ( SecurityException e ) {
				constructor = null;
			}
		}
		catch ( NoSuchMethodException ignore ) {
		}

		return constructor;
	}

	private static Map<EntityMode,Class<? extends EntityTuplizer>> buildBaseMapping() {
		Map<EntityMode,Class<? extends EntityTuplizer>> map = new ConcurrentHashMap<>();
		map.put( EntityMode.POJO, PojoEntityTuplizer.class );
		map.put( EntityMode.MAP, DynamicMapEntityTuplizer.class );
		return map;
	}
}
