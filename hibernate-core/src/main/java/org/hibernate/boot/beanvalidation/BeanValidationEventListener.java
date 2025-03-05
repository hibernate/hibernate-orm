/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.beanvalidation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Event listener used to enable Bean Validation for insert/update/delete events.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//FIXME review exception model
public class BeanValidationEventListener
		implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener, PreUpsertEventListener, PreCollectionUpdateEventListener {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			BeanValidationEventListener.class.getName()
	);

	private ValidatorFactory factory;
	private final ConcurrentHashMap<EntityPersister, Set<String>> associationsPerEntityPersister = new ConcurrentHashMap<>();
	private GroupsPerOperation groupsPerOperation;
	boolean initialized;

	/**
	 * Constructor used in an environment where validator factory is injected (JPA2).
	 *
	 * @param factory The {@code ValidatorFactory} to use to create {@code Validator} instance(s)
	 * @param settings Configured properties
	 */
	public BeanValidationEventListener(
			ValidatorFactory factory, Map<String,Object> settings, ClassLoaderService classLoaderService) {
		init( factory, settings, classLoaderService );
	}

	public void initialize(Map<String,Object> settings, ClassLoaderService classLoaderService) {
		if ( !initialized ) {
			ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
			init( factory, settings, classLoaderService );
		}
	}

	private void init(ValidatorFactory factory, Map<String,Object> settings, ClassLoaderService classLoaderService) {
		this.factory = factory;
		groupsPerOperation = GroupsPerOperation.from( settings, new ClassLoaderAccessImpl( classLoaderService ) );
		initialized = true;
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
		TraversableResolver tr = new HibernateTraversableResolver( persister, associationsPerEntityPersister, sessionFactory );
		Validator validator = factory.usingContext()
				.traversableResolver( tr )
				.getValidator();
		final Class<?>[] groups = groupsPerOperation.get( operation );
		if ( groups.length > 0 ) {
			final Set<ConstraintViolation<T>> constraintViolations = validator.validate( object, groups );
			if ( constraintViolations.size() > 0 ) {
				Set<ConstraintViolation<?>> propagatedViolations = CollectionHelper.setOfSize( constraintViolations.size() );
				Set<String> classNames = new HashSet<>();
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
