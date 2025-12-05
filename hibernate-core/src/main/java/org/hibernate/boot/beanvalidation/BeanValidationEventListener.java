/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.persister.entity.EntityPersister;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.hibernate.boot.beanvalidation.BeanValidationLogger.BEAN_VALIDATION_LOGGER;
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
		implements SessionFactoryObserver,
				PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener, PreUpsertEventListener,
				PreCollectionUpdateEventListener {

	private final HibernateTraversableResolver traversableResolver;
	private final Validator validator;
	private final GroupsPerOperation groupsPerOperation;

	private SessionFactoryImplementor sessionFactory;

	public BeanValidationEventListener(
			ValidatorFactory factory, Map<String, Object> settings, ClassLoaderService classLoaderService) {
		traversableResolver = new HibernateTraversableResolver();
		validator =
				factory.usingContext()
						.traversableResolver( traversableResolver )
						.getValidator();
		groupsPerOperation =
				GroupsPerOperation.from( settings,
						new ClassLoaderAccessImpl( classLoaderService ) );
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		sessionFactory = factory.unwrap( SessionFactoryImplementor.class );
		sessionFactory.getMappingMetamodel()
				.forEachEntityDescriptor( entityPersister ->
						traversableResolver.addPersister( entityPersister, sessionFactory ) );
	}

	public boolean onPreInsert(PreInsertEvent event) {
		validate(
				event.getEntity(),
				event.getPersister(),
				GroupsPerOperation.Operation.INSERT
		);
		return false;
	}

	public boolean onPreUpdate(PreUpdateEvent event) {
		validate(
				event.getEntity(),
				event.getPersister(),
				GroupsPerOperation.Operation.UPDATE
		);
		return false;
	}

	public boolean onPreDelete(PreDeleteEvent event) {
		validate(
				event.getEntity(),
				event.getPersister(),
				GroupsPerOperation.Operation.DELETE
		);
		return false;
	}

	@Override
	public boolean onPreUpsert(PreUpsertEvent event) {
		validate(
				event.getEntity(),
				event.getPersister(),
				GroupsPerOperation.Operation.UPSERT
		);
		return false;
	}

	@Override
	public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
		final Object entity = castNonNull( event.getCollection().getOwner() );
		validate(
				entity,
				getEntityPersister( event.getSession(), event.getAffectedOwnerEntityName(), entity ),
				GroupsPerOperation.Operation.UPDATE
		);
	}

	private EntityPersister getEntityPersister(
			SharedSessionContractImplementor session, String entityName, Object entity) {
		if ( session != null ) {
			return session.getEntityPersister( entityName, entity );
		}
		else {
			final var metamodel = sessionFactory.getMappingMetamodel();
			return entityName == null
					? metamodel.getEntityDescriptor( entity.getClass().getName() )
					: metamodel.getEntityDescriptor( entityName )
							.getSubclassEntityPersister( entity, sessionFactory );
		}
	}

	private <T> void validate(T object, EntityPersister persister, GroupsPerOperation.Operation operation) {
		if ( object != null
				&& persister.getRepresentationStrategy().getMode() == RepresentationMode.POJO ) {
			final var groups = groupsPerOperation.get( operation );
			if ( groups.length > 0 ) {
				final var constraintViolations = validator.validate( object, groups );
				if ( !constraintViolations.isEmpty() ) {
					final Set<ConstraintViolation<?>> propagatedViolations =
							setOfSize( constraintViolations.size() );
					final Set<String> classNames = new HashSet<>();
					for ( var violation : constraintViolations ) {
						BEAN_VALIDATION_LOGGER.trace( violation );
						propagatedViolations.add( violation );
						classNames.add( violation.getLeafBean().getClass().getName() );
					}
					throw new ConstraintViolationException(
							message( operation, classNames, groups, constraintViolations ),
							propagatedViolations );
				}
			}
		}
	}

	private <T> String message(
			GroupsPerOperation.Operation operation,
			Set<String> classNames,
			Class<?>[] groups,
			Set<ConstraintViolation<T>> constraintViolations) {
		final var builder = new StringBuilder();
		builder.append( "Validation failed for classes " )
				.append( classNames )
				.append( " during " )
				.append( operation.getName() )
				.append( " time for groups [" );
		for ( var group : groups ) {
			builder.append( group.getName() ).append( ", " );
		}
		builder.append( "]\nList of constraint violations:[\n" );
		for ( var violation : constraintViolations ) {
			builder.append( "\t" )
					.append( violation.toString() )
					.append( "\n" );
		}
		builder.append( "]" );
		return builder.toString();
	}
}
