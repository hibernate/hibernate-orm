package org.hibernate.cfg.beanvalidation;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hibernate.EntityMode;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Emmanuel Bernard
 */
//FIXME review exception model
public class BeanValidationEventListener implements
		PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener {


	private ValidatorFactory factory;
	private ConcurrentHashMap<EntityPersister, Set<String>> associationsPerEntityPersister =
			new ConcurrentHashMap<EntityPersister, Set<String>>();
	private GroupsPerOperation groupsPerOperation;


	public BeanValidationEventListener(ValidatorFactory factory, Properties properties) {
		this.factory = factory;
		groupsPerOperation = new GroupsPerOperation(properties);
	}



	public boolean onPreInsert(PreInsertEvent event) {
		validate( event.getEntity(), event.getSession().getEntityMode(), event.getPersister(),
				event.getSession().getFactory(), GroupsPerOperation.Operation.INSERT );
		return false;
	}

	public boolean onPreUpdate(PreUpdateEvent event) {
		validate( event.getEntity(), event.getSession().getEntityMode(), event.getPersister(),
				event.getSession().getFactory(), GroupsPerOperation.Operation.UPDATE );
		return false;
	}

	public boolean onPreDelete(PreDeleteEvent event) {
		validate( event.getEntity(), event.getSession().getEntityMode(), event.getPersister(),
				event.getSession().getFactory(),  GroupsPerOperation.Operation.DELETE );
		return false;
	}

	private <T> void validate(T object, EntityMode mode, EntityPersister persister,
							  SessionFactoryImplementor sessionFactory, GroupsPerOperation.Operation operation) {
		if ( object == null || mode != EntityMode.POJO ) return;
		TraversableResolver tr = new HibernateTraversableResolver( persister, associationsPerEntityPersister, sessionFactory );
		Validator validator = factory.usingContext()
										.traversableResolver( tr )
										.getValidator();
		final Class<?>[] groups = groupsPerOperation.get( operation );
		if ( groups.length > 0 ) {
			final Set<ConstraintViolation<T>> constraintViolations =
					validator.validate( object, groups );
			//FIXME CV should no longer be generics
			Object unsafeViolations = constraintViolations;
			if (constraintViolations.size() > 0 ) {
				//FIXME add Set<ConstraintViolation<?>>
				throw new ConstraintViolationException(
						"Invalid object at " + operation.getName() + " time for groups " + toString( groups ),
						(Set<ConstraintViolation<?>>) unsafeViolations);
			}
		}
	}

	private String toString(Class<?>[] groups) {
		StringBuilder toString = new StringBuilder( "[");
		for ( Class<?> group : groups ) {
			toString.append( group.getName() ).append( ", " );
		}
		toString.append( "]" );
		return toString.toString();
	}



}
