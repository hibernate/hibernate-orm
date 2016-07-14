/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sqm.ConsumerContext;
import org.hibernate.sqm.domain.DomainMetamodel;

/**
 * @author Steve Ebersole
 */
public class ConsumerContextImpl implements ConsumerContext {
	private final SessionFactoryImplementor sessionFactory;
	private final ClassLoaderService classLoaderService;

	public ConsumerContextImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.classLoaderService = sessionFactory.getServiceRegistry().getService( ClassLoaderService.class );
	}

	@Override
	public DomainMetamodel getDomainMetamodel() {
		return sessionFactory.getMetamodel();
	}

	@Override
	public Class classByName(String name) throws ClassNotFoundException {
		try {
			return classLoaderService.classForName( name );
		}
		catch (ClassLoadingException e) {
			try {
				final String importedName = sessionFactory.getMetamodel().getImportedClassName( name );
				if ( importedName != null ) {
					return classLoaderService.classForName( importedName );
				}
			}
			catch ( Exception ignore) {
			}
			throw new ClassNotFoundException( "Could not locate class : " + name, e );
		}
	}

	@Override
	public boolean useStrictJpaCompliance() {
		return sessionFactory.getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance();
	}
}
