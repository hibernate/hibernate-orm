/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dirtiness;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.MutationQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey(value = "HHH-11866")
@DomainModel(
		annotatedClasses = {HHH11866Test.Document.class})
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
				@Setting(name = AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY,
						value = "org.hibernate.orm.test.dirtiness.HHH11866Test$EntityDirtinessStrategy")
		}
)
@SessionFactory
public class HHH11866Test {

	@Test
	void hhh11866Test(SessionFactoryScope scope) {

		// prepare document
		scope.inTransaction( session -> {

			MutationQuery nativeMutationQuery = session.createNativeMutationQuery(
					"insert into Document (id,name) values (1,'title')" );
			nativeMutationQuery.executeUpdate();

		} );

		// assert document
		scope.inTransaction( session -> {

			final Document document = session.createQuery( "select d from Document d", Document.class )
					.getSingleResult();
			assertNotNull( document );
			assertEquals( "title", document.getName() );

			// check that flush doesn't trigger an update
			assertEquals( 0, scope.getSessionFactory().getStatistics().getEntityUpdateCount() );
			session.flush();
			assertEquals( 0, scope.getSessionFactory().getStatistics().getEntityUpdateCount() );
		} );
	}

	@Entity(name = "Document")
	public static class Document extends SelfDirtyCheckingEntity {

		@Id
		@GeneratedValue
		Long id;

		//  we need AccessType.PROPERTY to ensure that markDirtyProperty() is called
		@Access(AccessType.PROPERTY)
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			markDirtyProperty();
		}
	}

	public static class EntityDirtinessStrategy implements CustomEntityDirtinessStrategy {

		@Override
		public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
			return entity instanceof SelfDirtyCheckingEntity;
		}

		@Override
		public boolean isDirty(Object entity, EntityPersister persister, Session session) {
			return !cast( entity ).getDirtyProperties().isEmpty();
		}

		@Override
		public void resetDirty(Object entity, EntityPersister persister, Session session) {
			cast( entity ).clearDirtyProperties();
		}

		@Override
		public void findDirty(Object entity, EntityPersister persister, Session session, DirtyCheckContext dirtyCheckContext) {
			final SelfDirtyCheckingEntity dirtyAware = cast( entity );
			dirtyCheckContext.doDirtyChecking(
					attributeInformation -> {
						String propertyName = attributeInformation.getName();
						return dirtyAware.getDirtyProperties().contains( propertyName );
					}
			);
		}

		private SelfDirtyCheckingEntity cast(Object entity) {
			return (SelfDirtyCheckingEntity) entity;
		}
	}

	public static abstract class SelfDirtyCheckingEntity {

		private final Map<String, String> setterToPropertyMap = new HashMap<>();

		@Transient
		private final Set<String> dirtyProperties = new LinkedHashSet<>();

		public SelfDirtyCheckingEntity() {
			try {
				BeanInfo beanInfo = Introspector.getBeanInfo( getClass() );
				PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
				for ( PropertyDescriptor descriptor : descriptors ) {
					Method setter = descriptor.getWriteMethod();
					if ( setter != null ) {
						setterToPropertyMap.put( setter.getName(), descriptor.getName() );
					}
				}
			}
			catch (IntrospectionException e) {
				throw new IllegalStateException( e );
			}
		}

		public Set<String> getDirtyProperties() {
			return dirtyProperties;
		}

		public void clearDirtyProperties() {
			dirtyProperties.clear();
		}

		protected void markDirtyProperty() {
			String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
			dirtyProperties.add( setterToPropertyMap.get( methodName ) );
		}
	}
}
