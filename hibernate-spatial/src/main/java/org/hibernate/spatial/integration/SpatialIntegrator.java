package org.hibernate.spatial.integration;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.spatial.GeometryType;
import org.hibernate.type.TypeResolver;

import java.lang.reflect.Field;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class SpatialIntegrator implements Integrator {

     private static final String UNLOCK_ERROR_MSG = "SpatialIntegrator failed to unlock BasicTypeRegistry";

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        addType(sessionFactory.getTypeResolver());
    }

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        addType(metadata.getTypeResolver());
    }

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		//do nothing.
	}

    private void addType(TypeResolver typeResolver) {
        unlock(typeResolver);
        typeResolver.registerTypeOverride(GeometryType.INSTANCE);
        lock(typeResolver);
    }
    
    private void lock(TypeResolver typeResolver) {
        setLocked(typeResolver, true);
    }

    private void unlock(TypeResolver typeResolver) {
        setLocked(typeResolver, false);
    }

     private void setLocked(TypeResolver typeResolver, boolean locked) {
         try {
             Field registryFld = typeResolver.getClass().getDeclaredField("basicTypeRegistry");
             registryFld.setAccessible(true);
             Object registry = registryFld.get(typeResolver);
             Field lockedFld = registry.getClass().getDeclaredField("locked");
             lockedFld.setAccessible(true);
             lockedFld.setBoolean(registry, locked);
             lockedFld.setAccessible(false);
             registryFld.setAccessible(true);
         } catch (NoSuchFieldException e) {
             throw new IllegalStateException(UNLOCK_ERROR_MSG, e);
         } catch (IllegalAccessException e) {
             throw new IllegalStateException(UNLOCK_ERROR_MSG, e);
         }
 
     }
}
