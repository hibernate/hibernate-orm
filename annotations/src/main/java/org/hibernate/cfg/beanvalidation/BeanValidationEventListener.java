package org.hibernate.cfg.beanvalidation;

import java.util.Set;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import javax.validation.ValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ConstraintViolationException;
import javax.validation.groups.Default;

import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.util.ReflectHelper;

/**
 * @author Emmanuel Bernard
 */
//FIXME review exception model
public class BeanValidationEventListener implements
		PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener {
	private static final String JPA_GROUP_PREFIX = "javax.persistence.validation.group.";
	private static final Class<?>[] DEFAULT_GROUPS = new Class<?>[] { Default.class };
	private static final Class<?>[] EMPTY_GROUPS = new Class<?>[] { };

	private ValidatorFactory factory;
	private TraversableResolver tr;
	private Map<Operation, Class<?>[]> groupsPerOperation = new HashMap<Operation, Class<?>[]>(3);


	public BeanValidationEventListener(ValidatorFactory factory, Properties properties) {
		this.factory = factory;
		setGroupsForOperation( Operation.INSERT, properties );
		setGroupsForOperation( Operation.UPDATE, properties );
		setGroupsForOperation( Operation.DELETE, properties );
	}

	private void setGroupsForOperation(Operation operation, Properties properties) {
		Object property = properties.get( JPA_GROUP_PREFIX + operation.getGroupPropertyName() );

		Class<?>[] groups;
		if ( property == null ) {
			groups = operation == Operation.DELETE ? EMPTY_GROUPS : DEFAULT_GROUPS;
		}
		else {
			if ( property instanceof String ) {
				String stringProperty = (String) property;
				String[] groupNames = stringProperty.split( "," );
				if ( groupNames.length == 1 && groupNames[0].equals( "" ) ) {
					groups = EMPTY_GROUPS;
				}
				else {
					List<Class<?>> groupsList = new ArrayList<Class<?>>(groupNames.length);
					for (String groupName : groupNames) {
						String cleanedGroupName = groupName.trim();
						if ( cleanedGroupName.length() > 0) {
							try {
								groupsList.add( ReflectHelper.classForName( cleanedGroupName ) );
							}
							catch ( ClassNotFoundException e ) {
								throw new HibernateException( "Unable to load class " + cleanedGroupName, e );
							}
						}

					}
					groups = groupsList.toArray( new Class<?>[groupsList.size()] );
				}
			}
			else if ( property instanceof Class<?>[] ) {
				groups = (Class<?>[]) property;
			}
			else {
				//null is bad and excluded by instanceof => exception is raised
				throw new HibernateException( JPA_GROUP_PREFIX + operation.getGroupPropertyName() + " is of unknown type: String or Class<?>[] only");
			}
		}
		groupsPerOperation.put( operation, groups );
	}

	public boolean onPreInsert(PreInsertEvent event) {
		validate( event.getEntity(), event.getSession().getEntityMode(), Operation.INSERT );
		return false;
	}

	public boolean onPreUpdate(PreUpdateEvent event) {
		validate( event.getEntity(), event.getSession().getEntityMode(), Operation.UPDATE );
		return false;
	}

	public boolean onPreDelete(PreDeleteEvent event) {
		validate( event.getEntity(), event.getSession().getEntityMode(), Operation.DELETE );
		return false;
	}

	private <T> void validate(T object, EntityMode mode, Operation operation) {
		if ( object == null || mode != EntityMode.POJO ) return;
		Validator validator = factory.usingContext()
										//.traversableResolver( tr )
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
						(Set<ConstraintViolation>) unsafeViolations);
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

	private static enum Operation {
		INSERT("persist", "pre-persist"),
		UPDATE("update", "pre-update"),
		DELETE("remove", "pre-remove");

		private String exposedName;
		private String groupPropertyName;

		Operation(String exposedName, String groupProperty) {
			this.exposedName = exposedName;
			this.groupPropertyName = groupProperty;
		}

		public String getName() {
			return exposedName;
		}

		public String getGroupPropertyName() {
			return groupPropertyName;
		}
	}

}
