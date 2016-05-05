/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package org.hibernate.boot.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryImpl;

import static org.hibernate.internal.SessionFactoryImpl.HIBERNATE_DEEPSERIALIZE;

/**
 * Caches Session Factory to file to speed up startup time.
 *
 * @author kedzie
 */
public class CachedSessionFactoryBuilder extends AbstractDelegatingSessionFactoryBuilder<CachedSessionFactoryBuilder> {
   private static final CoreMessageLogger log = CoreLogging.messageLogger( CachedSessionFactoryBuilder.class );

   public CachedSessionFactoryBuilder(SessionFactoryBuilderImplementor builder) {
      super(builder);
   }

   @Override
   public SessionFactory build() {
      final SessionFactoryBuilderImpl d = (SessionFactoryBuilderImpl) delegate;
      final StandardServiceRegistryImpl registry = (StandardServiceRegistryImpl) d.getServiceRegistry();
      final ConfigurationService cfg = registry.getService( ConfigurationService.class );
      String cacheFileSetting = (String) cfg.getSettings().get( AvailableSettings.SESSION_FACTORY_CACHE_FILE );
      File metadataCache = new File( cacheFileSetting );
      FileInputStream fis = null;
      FileOutputStream fos = null;
      try {
         if ( !metadataCache.exists() ) {
            log.debugf("Building Session Factory...");
            System.setProperty( HIBERNATE_DEEPSERIALIZE, "true" );
            SessionFactoryImpl sf =  (SessionFactoryImpl)d.build();
            fos = new FileOutputStream( metadataCache );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( sf );
            return sf;
         } else {
            log.debugf( "Reading Session Factory... {}", cacheFileSetting );
            final ClassLoaderService cl = registry.getService( ClassLoaderService.class );
            fis = new FileInputStream( metadataCache );
            ObjectInputStream ois = new ObjectInputStream( fis ) {
               {
                  enableResolveObject( true );
               }

               @Override
               protected Object resolveObject(Object obj) throws IOException {
                  if( obj instanceof ClassLoaderServiceImpl ) {
                     log.tracef( "Overriding ClassLoaderServiceImpl with {}", cl );
                     return cl;
                  }
                  return super.resolveObject( obj );
               }
            };
            return (SessionFactoryImpl) ois.readObject();
         }
      }catch(Exception e){
         throw new RuntimeException(e);
      } finally {
         try {
            if(fis!=null) {
               fis.close();
            }
            if(fos!=null) {
               fos.close();
            }
         } catch (IOException e) {}
      }
   }

   @Override
   protected CachedSessionFactoryBuilder getThis() {
      return this;
   }
}
