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

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.ServiceRegistry;

/**
 * Caches Session Factory to file to speed up startup time.
 *
 * @author kedzie
 */
public class CachedSessionFactoryBuilder extends AbstractDelegatingSessionFactoryBuilder<CachedSessionFactoryBuilder> {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( CachedSessionFactoryBuilder.class );

	private ServiceRegistry registry;

	public CachedSessionFactoryBuilder(SessionFactoryBuilderImplementor builder, ServiceRegistry registry) {
		super( builder );
		this.registry = registry;
	}

	@Override
	public SessionFactory build() {
		final SessionFactoryBuilderImpl delegate = (SessionFactoryBuilderImpl) delegate();

		final ConfigurationService cfg = registry.getService( ConfigurationService.class );

		String sessionFactorySerializationFilePath = (String) cfg.getSettings()
				.get( AvailableSettings.SESSION_FACTORY_SERIALIZATION_FILE );

		File sessionFactorySerializationFile = new File( sessionFactorySerializationFilePath );
		try {
			if ( !sessionFactorySerializationFile.exists() || sessionFactorySerializationFile.length() == 0 ) {
				log.debugf( "Building Session Factory..." );

				try(FileOutputStream fos = new FileOutputStream( sessionFactorySerializationFile )) {
					SessionFactoryImpl sf = (SessionFactoryImpl) delegate.build();
					ObjectOutputStream oos = new ObjectOutputStream( fos );
					oos.writeObject( sf );
					return sf;
				}
			}
			else {
				log.debugf( "Reading Session Factory... {}", sessionFactorySerializationFilePath );
				final ClassLoaderService cl = registry.getService( ClassLoaderService.class );
				try (FileInputStream fis = new FileInputStream( sessionFactorySerializationFile )) {
					ObjectInputStream ois = new ObjectInputStream( fis ) {
						{
							enableResolveObject( true );
						}

						@Override
						protected Object resolveObject(Object obj) throws IOException {
							if ( obj instanceof ClassLoaderServiceImpl ) {
								log.tracef( "Overriding ClassLoaderServiceImpl with {}", cl );
								return cl;
							}
							return super.resolveObject( obj );
						}
					};
					return (SessionFactoryImpl) ois.readObject();
				}
			}
		}
		catch (Exception e) {
			throw new HibernateException( e );
		}
	}

	@Override
	protected CachedSessionFactoryBuilder getThis() {
		return this;
	}
}
