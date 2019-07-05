/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.beanvalidation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.groups.Default;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.ClassLoaderAccess;

/**
 * @author Emmanuel Bernard
 */
public class GroupsPerOperation {
	private static final String JPA_GROUP_PREFIX = "javax.persistence.validation.group.";
	private static final String HIBERNATE_GROUP_PREFIX = "org.hibernate.validator.group.";

	private static final Class<?>[] DEFAULT_GROUPS = new Class<?>[] { Default.class };
	private static final Class<?>[] EMPTY_GROUPS = new Class<?>[] { };

	private Map<Operation, Class<?>[]> groupsPerOperation = new HashMap<Operation, Class<?>[]>(4);

	private GroupsPerOperation() {
	}

	public static GroupsPerOperation from(Map settings, ClassLoaderAccess classLoaderAccess) {
		GroupsPerOperation groupsPerOperation = new GroupsPerOperation();

		applyOperationGrouping( groupsPerOperation, Operation.INSERT, settings, classLoaderAccess );
		applyOperationGrouping( groupsPerOperation, Operation.UPDATE, settings, classLoaderAccess );
		applyOperationGrouping( groupsPerOperation, Operation.DELETE, settings, classLoaderAccess );
		applyOperationGrouping( groupsPerOperation, Operation.DDL, settings, classLoaderAccess );

		return groupsPerOperation;
	}

	private static void applyOperationGrouping(
			GroupsPerOperation groupsPerOperation,
			Operation operation,
			Map settings,
			ClassLoaderAccess classLoaderAccess) {
		groupsPerOperation.groupsPerOperation.put(
				operation,
				buildGroupsForOperation( operation, settings, classLoaderAccess )
		);
	}

	public static Class<?>[] buildGroupsForOperation(Operation operation, Map settings, ClassLoaderAccess classLoaderAccess) {
		final Object property = settings.get( operation.getGroupPropertyName() );

		if ( property == null ) {
			return operation == Operation.DELETE ? EMPTY_GROUPS : DEFAULT_GROUPS;
		}

		if ( property instanceof Class<?>[] ) {
			return (Class<?>[]) property;
		}

		if ( property instanceof String ) {
			String stringProperty = (String) property;
			String[] groupNames = stringProperty.split( "," );
			if ( groupNames.length == 1 && groupNames[0].isEmpty() ) {
				return EMPTY_GROUPS;
			}

			List<Class<?>> groupsList = new ArrayList<Class<?>>(groupNames.length);
			for (String groupName : groupNames) {
				String cleanedGroupName = groupName.trim();
				if ( cleanedGroupName.length() > 0) {
					try {
						groupsList.add( classLoaderAccess.classForName( cleanedGroupName ) );
					}
					catch ( ClassLoadingException e ) {
						throw new HibernateException( "Unable to load class " + cleanedGroupName, e );
					}
				}
			}
			return groupsList.toArray( new Class<?>[groupsList.size()] );
		}

		//null is bad and excluded by instanceof => exception is raised
		throw new HibernateException( JPA_GROUP_PREFIX + operation.getGroupPropertyName() + " is of unknown type: String or Class<?>[] only");
	}

	public Class<?>[] get(Operation operation) {
		return groupsPerOperation.get( operation );
	}

	public static enum Operation {
		INSERT("persist", JPA_GROUP_PREFIX + "pre-persist"),
		UPDATE("update", JPA_GROUP_PREFIX + "pre-update"),
		DELETE("remove", JPA_GROUP_PREFIX + "pre-remove"),
		DDL("ddl", HIBERNATE_GROUP_PREFIX + "ddl");


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
