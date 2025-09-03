/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.PreUpsertEvent;
import org.hibernate.event.spi.PreUpsertEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;

/**
 * Event listener used to enable Bean Validation for insert/update/delete events.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//FIXME review exception model
public class BeanValidationEventListener
		implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener, PreUpsertEventListener, PreCollectionUpdateEventListener,
		SessionFactoryObserver {

	private static final CoreMessageLogger log = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			BeanValidationEventListener.class.getName()
	);

	private final HibernateTraversableResolver traversableResolver;
	private final Validator validator;
	private final GroupsPerOperation groupsPerOperation;

	public BeanValidationEventListener(
			ValidatorFactory factory, Map<String, Object> settings, ClassLoaderService classLoaderService) {
		traversableResolver = new HibernateTraversableResolver();
		validator = factory.usingContext()
				.traversableResolver( traversableResolver )
				.getValidator();
		groupsPerOperation = GroupsPerOperation.from( settings, new ClassLoaderAccessImpl( classLoaderService ) );
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		SessionFactoryImplementor implementor = factory.unwrap( SessionFactoryImplementor.class );
		implementor
				.getMappingMetamodel()
				.forEachEntityDescriptor( entityPersister -> traversableResolver.addPersister( entityPersister, implementor ) );
	}

	public boolean onPreInsert(PreInsertEvent event) {
		validate(
				event.getEntity(),
				event.getPersister(),
				event.getFactory(),
				GroupsPerOperation.Operation.INSERT
		);
		return false;
	}

	public boolean onPreUpdate(PreUpdateEvent event) {
		validate(
				event.getEntity(),
				event.getPersister(),
				event.getFactory(),
				GroupsPerOperation.Operation.UPDATE
		);
		return false;
	}

	public boolean onPreDelete(PreDeleteEvent event) {
		validate(
				event.getEntity(),
				event.getPersister(),
				event.getFactory(),
				GroupsPerOperation.Operation.DELETE
		);
		return false;
	}

	@Override
	public boolean onPreUpsert(PreUpsertEvent event) {
		validate(
				event.getEntity(),
				event.getPersister(),
				event.getFactory(),
				GroupsPerOperation.Operation.UPSERT
		);
		return false;
	}

	@Override
	public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
		final Object entity = castNonNull( event.getCollection().getOwner() );
		validate(
				entity,
				event.getSession().getEntityPersister( event.getAffectedOwnerEntityName(), entity ),
				event.getFactory(),
				GroupsPerOperation.Operation.UPDATE
		);
	}

	private <T> void validate(
			T object,
			EntityPersister persister,
			SessionFactoryImplementor sessionFactory,
			GroupsPerOperation.Operation operation) {
		if ( object == null || persister.getRepresentationStrategy().getMode() != RepresentationMode.POJO ) {
			return;
		}
		final Class<?>[] groups = groupsPerOperation.get( operation );
		if ( groups.length > 0 ) {
			final Set<ConstraintViolation<T>> constraintViolations = validator.validate( object, groups );
			if ( !constraintViolations.isEmpty() ) {
				final Set<ConstraintViolation<?>> propagatedViolations = setOfSize( constraintViolations.size() );
				final Set<String> classNames = new HashSet<>();
				for ( ConstraintViolation<?> violation : constraintViolations ) {
					log.trace( violation );
					propagatedViolations.add( violation );
					classNames.add( violation.getLeafBean().getClass().getName() );
				}
				final StringBuilder builder = new StringBuilder();
				builder.append( "Validation failed for classes " );
				builder.append( classNames );
				builder.append( " during " );
				builder.append( operation.getName() );
				builder.append( " time for groups " );
				builder.append( toString( groups ) );
				builder.append( "\nList of constraint violations:[\n" );
				for ( ConstraintViolation<?> violation : constraintViolations ) {
					builder.append( "\t" ).append( violation.toString() ).append( "\n" );
				}
				builder.append( "]" );

				throw new ConstraintViolationException( builder.toString(), propagatedViolations );
			}
		}
	}

	private String toString(Class<?>[] groups) {
		final StringBuilder toString = new StringBuilder( "[" );
		for ( Class<?> group : groups ) {
			toString.append( group.getName() ).append( ", " );
		}
		toString.append( "]" );
		return toString.toString();
	}
}
