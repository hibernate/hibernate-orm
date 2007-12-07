/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.junit.functional;

import java.util.Iterator;
import java.sql.Blob;
import java.sql.Clob;

import org.hibernate.dialect.Dialect;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Collection;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class ExecutionEnvironment {

	public static final Dialect DIALECT = Dialect.getDialect();

	private final ExecutionEnvironment.Settings settings;

	private Configuration configuration;
	private SessionFactory sessionFactory;
	private boolean allowRebuild;

	public ExecutionEnvironment(ExecutionEnvironment.Settings settings) {
		this.settings = settings;
	}

	public boolean isAllowRebuild() {
		return allowRebuild;
	}

	public void setAllowRebuild(boolean allowRebuild) {
		this.allowRebuild = allowRebuild;
	}

	public Dialect getDialect() {
		return DIALECT;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void initialize() {
		if ( sessionFactory != null ) {
			throw new IllegalStateException( "attempt to initialize already initialized ExecutionEnvironment" );
		}
		if ( ! settings.appliesTo( getDialect() ) ) {
			return;
		}

		Configuration configuration = new Configuration();
		configuration.setProperty( Environment.CACHE_PROVIDER, "org.hibernate.cache.HashtableCacheProvider" );

		settings.configure( configuration );

		applyMappings( configuration );
		applyCacheSettings( configuration );


		if ( settings.createSchema() ) {
			configuration.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		}

		// make sure we use the same dialect...
		configuration.setProperty( Environment.DIALECT, getDialect().getClass().getName() );

		configuration.buildMappings();
		settings.afterConfigurationBuilt( configuration.createMappings(), getDialect() );

		SessionFactory sessionFactory = configuration.buildSessionFactory();
		this.configuration = configuration;
		this.sessionFactory = sessionFactory;

		settings.afterSessionFactoryBuilt( ( SessionFactoryImplementor ) sessionFactory );
	}

	private void applyMappings(Configuration configuration) {
		String[] mappings = settings.getMappings();
		for ( int i = 0; i < mappings.length; i++ ) {
			configuration.addResource( settings.getBaseForMappings() + mappings[i], ExecutionEnvironment.class.getClassLoader() );
		}
	}

	private void applyCacheSettings(Configuration configuration) {
		if ( settings.getCacheConcurrencyStrategy() != null ) {
			Iterator iter = configuration.getClassMappings();
			while ( iter.hasNext() ) {
				PersistentClass clazz = (PersistentClass) iter.next();
				Iterator props = clazz.getPropertyClosureIterator();
				boolean hasLob = false;
				while ( props.hasNext() ) {
					Property prop = (Property) props.next();
					if ( prop.getValue().isSimpleValue() ) {
						String type = ( ( SimpleValue ) prop.getValue() ).getTypeName();
						if ( "blob".equals(type) || "clob".equals(type) ) {
							hasLob = true;
						}
						if ( Blob.class.getName().equals(type) || Clob.class.getName().equals(type) ) {
							hasLob = true;
						}
					}
				}
				if ( !hasLob && !clazz.isInherited() && settings.overrideCacheStrategy() ) {
					configuration.setCacheConcurrencyStrategy( clazz.getEntityName(), settings.getCacheConcurrencyStrategy() );
				}
			}
			iter = configuration.getCollectionMappings();
			while ( iter.hasNext() ) {
				Collection coll = (Collection) iter.next();
				configuration.setCollectionCacheConcurrencyStrategy( coll.getRole(), settings.getCacheConcurrencyStrategy() );
			}
		}
	}

	public void rebuild() {
		if ( !allowRebuild ) {
			return;
		}
		if ( sessionFactory != null ) {
			sessionFactory.close();
			sessionFactory = null;
		}
		sessionFactory = configuration.buildSessionFactory();
		settings.afterSessionFactoryBuilt( ( SessionFactoryImplementor ) sessionFactory );
	}

	public void complete() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
			sessionFactory = null;
		}
		configuration = null;
	}

	public static interface Settings {
		public String[] getMappings();
		public String getBaseForMappings();
		public boolean createSchema();
		public boolean recreateSchemaAfterFailure();
		public void configure(Configuration cfg);
		public boolean overrideCacheStrategy();
		public String getCacheConcurrencyStrategy();
		public void afterSessionFactoryBuilt(SessionFactoryImplementor sfi);
		public void afterConfigurationBuilt(Mappings mappings, Dialect dialect);
		public boolean appliesTo(Dialect dialect);
	}
}
