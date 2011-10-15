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
package org.hibernate.cfg.beanvalidation;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.jboss.logging.Logger;

import org.hibernate.EntityMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Event listener used to enable Bean Validation for insert/update/delete events.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//FIXME review exception model
public class BeanValidationEventListener
		implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       BeanValidationEventListener.class.getName());

	private ValidatorFactory factory;
	private ConcurrentHashMap<EntityPersister, Set<String>> associationsPerEntityPersister =
			new ConcurrentHashMap<EntityPersister, Set<String>>();
	private GroupsPerOperation groupsPerOperation;
	boolean initialized;

	/**
	 * No-arg constructor used when listener is configured via configuration file
	 */
	public BeanValidationEventListener() {
	}

	/**
	 * Constructor used in an environment where validator factory is injected (JPA2).
	 *
	 * @param factory The {@code ValidatorFactory} to use to create {@code Validator} instance(s)
	 * @param properties Configued properties
	 */
	public BeanValidationEventListener(ValidatorFactory factory, Properties properties) {
		init( factory, properties );
	}

	public void initialize(Configuration cfg) {
		if ( !initialized ) {
			ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
			Properties props = cfg.getProperties();
			init( factory, props );
		}
	}

	public boolean onPreInsert(PreInsertEvent event) {
		validate(
				event.getEntity(), event.getPersister().getEntityMode(), event.getPersister(),
				event.getSession().getFactory(), GroupsPerOperation.Operation.INSERT
		);
		return false;
	}

	public boolean onPreUpdate(PreUpdateEvent event) {
		validate(
				event.getEntity(), event.getPersister().getEntityMode(), event.getPersister(),
				event.getSession().getFactory(), GroupsPerOperation.Operation.UPDATE
		);
		return false;
	}

	public boolean onPreDelete(PreDeleteEvent event) {
		validate(
				event.getEntity(), event.getPersister().getEntityMode(), event.getPersister(),
				event.getSession().getFactory(), GroupsPerOperation.Operation.DELETE
		);
		return false;
	}

	private void init(ValidatorFactory factory, Properties properties) {
		this.factory = factory;
		groupsPerOperation = new GroupsPerOperation( properties );
		initialized = true;
	}

	private <T> void validate(T object, EntityMode mode, EntityPersister persister,
							  SessionFactoryImplementor sessionFactory, GroupsPerOperation.Operation operation) {
		if ( object == null || mode != EntityMode.POJO ) {
			return;
		}
		TraversableResolver tr = new HibernateTraversableResolver(
				persister, associationsPerEntityPersister, sessionFactory
		);
		Validator validator = factory.usingContext()
				.traversableResolver( tr )
				.getValidator();
		final Class<?>[] groups = groupsPerOperation.get( operation );
		if ( groups.length > 0 ) {
			final Set<ConstraintViolation<T>> constraintViolations = validator.validate( object, groups );
			if ( constraintViolations.size() > 0 ) {
				Set<ConstraintViolation<?>> propagatedViolations =
						new HashSet<ConstraintViolation<?>>( constraintViolations.size() );
				Set<String> classNames = new HashSet<String>();
				for ( ConstraintViolation<?> violation : constraintViolations ) {
					LOG.trace( violation );
					propagatedViolations.add( violation );
					classNames.add( violation.getLeafBean().getClass().getName() );
				}
				StringBuilder builder = new StringBuilder();
				builder.append( "Validation failed for classes " );
				builder.append( classNames );
				builder.append( " during " );
				builder.append( operation.getName() );
				builder.append( " time for groups " );
				builder.append( toString( groups ) );
				builder.append( "\nList of constraint violations:[\n" );
				for (ConstraintViolation<?> violation : constraintViolations) {
					builder.append( "\t" ).append( violation.toString() ).append("\n");
				}
				builder.append( "]" );

				throw new ConstraintViolationException(
						builder.toString(), propagatedViolations
				);
			}
		}
	}

	private String toString(Class<?>[] groups) {
		StringBuilder toString = new StringBuilder( "[" );
		for ( Class<?> group : groups ) {
			toString.append( group.getName() ).append( ", " );
		}
		toString.append( "]" );
		return toString.toString();
	}
}
