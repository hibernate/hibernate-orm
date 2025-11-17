/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.type;

import java.net.URL;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Type;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleTests {
	@Test
	void testProperUsage() {
		final ExtendedBeanManagerImpl extendedBeanManager = new ExtendedBeanManagerImpl();

		final StandardServiceRegistryBuilder ssrbBuilder = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, extendedBeanManager );

		try ( final StandardServiceRegistry ssr = ssrbBuilder.build() ) {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( MappedEntity.class )
					.buildMetadata();

			final PersistentClass entityBinding = metadata.getEntityBinding( MappedEntity.class.getName() );
			final Property property = entityBinding.getProperty( "url" );
			assertThat( property ).isNotNull();
			assertThat( property.getValue() ).isInstanceOf( BasicValue.class );
			final BasicValue.Resolution<?> resolution = ( (BasicValue) property.getValue() ).getResolution();
			assertThat( resolution ).isNotNull();
			assertThat( resolution ).isInstanceOf( UserTypeResolution.class );
//			assertThat( ( (UserTypeResolution) resolution ).isResolved() ).isFalse();

			final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
					.disableDiscovery()
					.addBeanClasses( UrlType.class, OtherBean.class );
			try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
				final BeanManager beanManager = cdiContainer.getBeanManager();
				extendedBeanManager.injectBeanManager( beanManager );
			}

			try ( final SessionFactory sf = metadata.buildSessionFactory() ) {
				sf.inSession( (session) -> {
					session.createSelectionQuery( "from MappedEntity" ).list();
				} );
			}
		}

	}

	@Entity( name = "MappedEntity" )
	@Table( name = "mapped_entity" )
	public static class MappedEntity {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Basic
		@Type( UrlType.class )
		private URL url;

		protected MappedEntity() {
			// for use by Hibernate
		}

		public MappedEntity(Integer id, String name, URL url) {
			this.id = id;
			this.name = name;
			this.url = url;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public URL getUrl() {
			return url;
		}

		public void setUrl(URL url) {
			this.url = url;
		}
	}
}
