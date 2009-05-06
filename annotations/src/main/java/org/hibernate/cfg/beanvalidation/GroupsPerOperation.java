package org.hibernate.cfg.beanvalidation;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.validation.groups.Default;

import org.hibernate.util.ReflectHelper;
import org.hibernate.HibernateException;

/**
 * @author Emmanuel Bernard
 */
public class GroupsPerOperation {

	private static final String JPA_GROUP_PREFIX = "javax.persistence.validation.group.";
	private static final String HIBERNATE_GROUP_PREFIX = "org.hibernate.validator.group.";
	private static final Class<?>[] DEFAULT_GROUPS = new Class<?>[] { Default.class };
	private static final Class<?>[] EMPTY_GROUPS = new Class<?>[] { };

	private Map<Operation, Class<?>[]> groupsPerOperation = new HashMap<Operation, Class<?>[]>(4);

	public GroupsPerOperation(Properties properties) {
		setGroupsForOperation( Operation.INSERT, properties );
		setGroupsForOperation( Operation.UPDATE, properties );
		setGroupsForOperation( Operation.DELETE, properties );
		setGroupsForOperation( Operation.DDL, properties );
	}

	private void setGroupsForOperation(Operation operation, Properties properties) {
		Object property = properties.get( operation.getGroupPropertyName() );

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
