/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.ClassLoaderAccess;

import jakarta.validation.groups.Default;

import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * @author Emmanuel Bernard
 */
public class GroupsPerOperation {
	private static final String JPA_GROUP_PREFIX = "javax.persistence.validation.group.";
	private static final String JAKARTA_JPA_GROUP_PREFIX = "jakarta.persistence.validation.group.";
	private static final String HIBERNATE_GROUP_PREFIX = "org.hibernate.validator.group.";

	private static final Class<?>[] DEFAULT_GROUPS = new Class<?>[] { Default.class };
	private static final Class<?>[] EMPTY_GROUPS = new Class<?>[] { };

	private final Map<Operation, Class<?>[]> groupsPerOperation = mapOfSize( 4 );

	private GroupsPerOperation() {
	}

	public static GroupsPerOperation from(Map<String,Object> settings, ClassLoaderAccess classLoaderAccess) {
		final GroupsPerOperation groupsPerOperation = new GroupsPerOperation();
		applyOperationGrouping( groupsPerOperation, Operation.INSERT, settings, classLoaderAccess );
		applyOperationGrouping( groupsPerOperation, Operation.UPDATE, settings, classLoaderAccess );
		applyOperationGrouping( groupsPerOperation, Operation.DELETE, settings, classLoaderAccess );
		applyOperationGrouping( groupsPerOperation, Operation.UPSERT, settings, classLoaderAccess );
		applyOperationGrouping( groupsPerOperation, Operation.DDL, settings, classLoaderAccess );
		return groupsPerOperation;
	}

	private static void applyOperationGrouping(
			GroupsPerOperation groupsPerOperation,
			Operation operation,
			Map<String,Object> settings,
			ClassLoaderAccess classLoaderAccess) {
		groupsPerOperation.groupsPerOperation.put(
				operation,
				buildGroupsForOperation( operation, settings, classLoaderAccess )
		);
	}

	public static Class<?>[] buildGroupsForOperation(
			Operation operation, Map<String,Object> settings, ClassLoaderAccess classLoaderAccess) {
		Object property = settings.get( operation.getJakartaGroupPropertyName() );
		if ( property == null ) {
			property = settings.get( operation.getGroupPropertyName() );
		}

		if ( property == null ) {
			return operation == Operation.DELETE ? EMPTY_GROUPS : DEFAULT_GROUPS;
		}

		if ( property instanceof Class<?>[] classes ) {
			return classes;
		}

		if ( property instanceof String string ) {
			final String[] groupNames = split( ",", string );
			if ( groupNames.length == 1 && groupNames[0].isEmpty() ) {
				return EMPTY_GROUPS;
			}

			final List<Class<?>> groupsList = new ArrayList<>( groupNames.length );
			for ( String groupName : groupNames ) {
				final String cleanedGroupName = groupName.trim();
				if ( !cleanedGroupName.isEmpty() ) {
					try {
						groupsList.add( classLoaderAccess.classForName( cleanedGroupName ) );
					}
					catch ( ClassLoadingException e ) {
						throw new HibernateException( "Unable to load class " + cleanedGroupName, e );
					}
				}
			}
			return groupsList.toArray( new Class<?>[0] );
		}

		//null is bad and excluded by instanceof => exception is raised
		throw new HibernateException( JAKARTA_JPA_GROUP_PREFIX
				+ operation.getJakartaGroupPropertyName()
				+ " is of unknown type: String or Class<?>[] only");
	}

	public Class<?>[] get(Operation operation) {
		return groupsPerOperation.get( operation );
	}

	public enum Operation {
		INSERT( "persist", JPA_GROUP_PREFIX + "pre-persist", JAKARTA_JPA_GROUP_PREFIX + "pre-persist" ),
		UPDATE( "update", JPA_GROUP_PREFIX + "pre-update", JAKARTA_JPA_GROUP_PREFIX + "pre-update" ),
		DELETE( "remove", JPA_GROUP_PREFIX + "pre-remove", JAKARTA_JPA_GROUP_PREFIX + "pre-remove" ),
		UPSERT( "upsert", JPA_GROUP_PREFIX + "pre-upsert", JAKARTA_JPA_GROUP_PREFIX + "pre-upsert" ),
		DDL( "ddl", HIBERNATE_GROUP_PREFIX + "ddl", HIBERNATE_GROUP_PREFIX + "ddl" );


		private final String exposedName;
		private final String groupPropertyName;
		private final String jakartaGroupPropertyName;

		Operation(String exposedName, String groupProperty, String jakartaGroupPropertyName) {
			this.exposedName = exposedName;
			this.groupPropertyName = groupProperty;
			this.jakartaGroupPropertyName = jakartaGroupPropertyName;
		}

		public String getName() {
			return exposedName;
		}

		public String getGroupPropertyName() {
			return groupPropertyName;
		}

		public String getJakartaGroupPropertyName() {
			return jakartaGroupPropertyName;
		}
	}

}
