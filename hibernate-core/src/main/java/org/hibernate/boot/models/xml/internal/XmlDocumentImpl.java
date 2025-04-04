/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import jakarta.persistence.AccessType;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableInstantiatorRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJavaTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJdbcTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedHqlQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedStoredProcedureQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.xml.spi.XmlDocument;
import org.hibernate.internal.util.NullnessHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * @author Steve Ebersole
 */
public class XmlDocumentImpl implements XmlDocument {
	private final Origin origin;
	private final JaxbEntityMappingsImpl root;
	private final DefaultsImpl defaults;
	private final List<JaxbEntityImpl> entityMappings;
	private final List<JaxbMappedSuperclassImpl> mappedSuperclassMappings;
	private final List<JaxbEmbeddableImpl> embeddableMappings;
	private final List<JaxbConverterImpl> converters;
	private final List<JaxbConverterRegistrationImpl> converterRegistrations;
	private final List<JaxbJavaTypeRegistrationImpl> javaTypeRegistrations;
	private final List<JaxbJdbcTypeRegistrationImpl> jdbcTypeRegistrations;
	private final List<JaxbUserTypeRegistrationImpl> userTypeRegistrations;
	private final List<JaxbCompositeUserTypeRegistrationImpl> compositeUserTypeRegistrations;
	private final List<JaxbCollectionUserTypeRegistrationImpl> collectionUserTypeRegistrations;
	private final List<JaxbEmbeddableInstantiatorRegistrationImpl> embeddableInstantiatorRegistrations;
	private final Map<String, JaxbNamedHqlQueryImpl> jpaNamedQueries;
	private final Map<String, JaxbNamedNativeQueryImpl> jpaNamedNativeQueries;
	private final Map<String, JaxbHbmNamedQueryType> hibernateNamedQueries;
	private final Map<String, JaxbHbmNamedNativeQueryType> hibernateNamedNativeQueries;
	private final Map<String, JaxbNamedStoredProcedureQueryImpl> namedStoredProcedureQueries;

	private XmlDocumentImpl(
			Origin origin,
			JaxbEntityMappingsImpl root,
			DefaultsImpl defaults,
			List<JaxbEntityImpl> entityMappings,
			List<JaxbMappedSuperclassImpl> mappedSuperclassMappings,
			List<JaxbEmbeddableImpl> embeddableMappings,
			List<JaxbConverterImpl> converters,
			List<JaxbConverterRegistrationImpl> converterRegistrations,
			List<JaxbJavaTypeRegistrationImpl> javaTypeRegistrations,
			List<JaxbJdbcTypeRegistrationImpl> jdbcTypeRegistrations,
			List<JaxbUserTypeRegistrationImpl> userTypeRegistrations,
			List<JaxbCompositeUserTypeRegistrationImpl> compositeUserTypeRegistrations,
			List<JaxbCollectionUserTypeRegistrationImpl> collectionUserTypeRegistrations,
			List<JaxbEmbeddableInstantiatorRegistrationImpl> embeddableInstantiatorRegistrations,
			Map<String, JaxbNamedHqlQueryImpl> jpaNamedQueries,
			Map<String, JaxbNamedNativeQueryImpl> jpaNamedNativeQueries,
			Map<String, JaxbNamedStoredProcedureQueryImpl> namedStoredProcedureQueries,
			Map<String, JaxbHbmNamedQueryType> hibernateNamedQueries,
			Map<String, JaxbHbmNamedNativeQueryType> hibernateNamedNativeQueries) {
		this.origin = origin;
		this.root = root;
		this.defaults = defaults;
		this.entityMappings = entityMappings;
		this.mappedSuperclassMappings = mappedSuperclassMappings;
		this.embeddableMappings = embeddableMappings;
		this.converters = converters;
		this.converterRegistrations = converterRegistrations;
		this.javaTypeRegistrations = javaTypeRegistrations;
		this.jdbcTypeRegistrations = jdbcTypeRegistrations;
		this.userTypeRegistrations = userTypeRegistrations;
		this.compositeUserTypeRegistrations = compositeUserTypeRegistrations;
		this.collectionUserTypeRegistrations = collectionUserTypeRegistrations;
		this.embeddableInstantiatorRegistrations = embeddableInstantiatorRegistrations;
		this.jpaNamedQueries = jpaNamedQueries;
		this.jpaNamedNativeQueries = jpaNamedNativeQueries;
		this.namedStoredProcedureQueries = namedStoredProcedureQueries;
		this.hibernateNamedQueries = hibernateNamedQueries;
		this.hibernateNamedNativeQueries = hibernateNamedNativeQueries;
	}

	@Override
	public Origin getOrigin() {
		return origin;
	}

	@Override
	public JaxbEntityMappingsImpl getRoot() {
		return root;
	}

	@Override
	public Defaults getDefaults() {
		return defaults;
	}

	@Override
	public List<JaxbEntityImpl> getEntityMappings() {
		return entityMappings;
	}

		@Override
	public List<JaxbMappedSuperclassImpl> getMappedSuperclassMappings() {
		return mappedSuperclassMappings;
	}

	@Override
	public List<JaxbEmbeddableImpl> getEmbeddableMappings() {
		return embeddableMappings;
	}

	@Override
	public List<JaxbConverterImpl> getConverters() {
		return converters;
	}

	@Override
	public List<JaxbConverterRegistrationImpl> getConverterRegistrations() {
		return converterRegistrations;
	}

	@Override
	public List<JaxbJavaTypeRegistrationImpl> getJavaTypeRegistrations() {
		return javaTypeRegistrations;
	}

	@Override
	public List<JaxbJdbcTypeRegistrationImpl> getJdbcTypeRegistrations() {
		return jdbcTypeRegistrations;
	}

	@Override
	public List<JaxbUserTypeRegistrationImpl> getUserTypeRegistrations() {
		return userTypeRegistrations;
	}

	@Override
	public List<JaxbCompositeUserTypeRegistrationImpl> getCompositeUserTypeRegistrations() {
		return compositeUserTypeRegistrations;
	}

	@Override
	public List<JaxbCollectionUserTypeRegistrationImpl> getCollectionUserTypeRegistrations() {
		return collectionUserTypeRegistrations;
	}

	@Override
	public List<JaxbEmbeddableInstantiatorRegistrationImpl> getEmbeddableInstantiatorRegistrations() {
		return embeddableInstantiatorRegistrations;
	}

	@Override
	public Map<String, JaxbNamedHqlQueryImpl> getJpaNamedQueries() {
		return jpaNamedQueries;
	}

	@Override
	public Map<String, JaxbNamedNativeQueryImpl> getJpaNamedNativeQueries() {
		return jpaNamedNativeQueries;
	}

	@Override
	public Map<String, JaxbHbmNamedQueryType> getHibernateNamedQueries() {
		return hibernateNamedQueries;
	}

	@Override
	public Map<String, JaxbHbmNamedNativeQueryType> getHibernateNamedNativeQueries() {
		return hibernateNamedNativeQueries;
	}

	@Override
	public Map<String, JaxbNamedStoredProcedureQueryImpl> getNamedStoredProcedureQueries() {
		return namedStoredProcedureQueries;
	}

	private static class DefaultsImpl implements Defaults {
		private final String pckg;
		private final AccessType accessType;
		private final String accessorStrategy;
		private final String catalog;
		private final String schema;
		private final boolean autoImport;
		private final boolean impliedLaziness;

		private DefaultsImpl(
				String pckg,
				AccessType accessType,
				String accessorStrategy,
				String catalog,
				String schema,
				Boolean autoImport,
				Boolean impliedLaziness) {
			this.pckg = pckg;
			this.accessType = accessType;
			this.accessorStrategy = accessorStrategy;
			this.catalog = catalog;
			this.schema = schema;
			this.autoImport = NullnessHelper.nullif( autoImport, true );
			this.impliedLaziness = NullnessHelper.nullif( impliedLaziness, false );
		}

		@Override
		public String getPackage() {
			return pckg;
		}

		@Override
		public AccessType getAccessType() {
			return accessType;
		}

		@Override
		public String getAccessorStrategy() {
			return accessorStrategy;
		}

		@Override
		public String getCatalog() {
			return catalog;
		}

		@Override
		public String getSchema() {
			return schema;
		}

		@Override
		public boolean isAutoImport() {
			return autoImport;
		}

		@Override
		public boolean isLazinessImplied() {
			return impliedLaziness;
		}

		static DefaultsImpl consume(JaxbEntityMappingsImpl jaxbRoot, PersistenceUnitMetadata metadata) {
			return new DefaultsImpl(
					jaxbRoot.getPackage(),
					NullnessHelper.coalesce( jaxbRoot.getAccess(), metadata.getAccessType() ),
					NullnessHelper.coalesce( jaxbRoot.getAttributeAccessor(), metadata.getDefaultAccessStrategyName() ),
					jaxbRoot.getCatalog(),
					jaxbRoot.getSchema(),
					jaxbRoot.isAutoImport(),
					jaxbRoot.isDefaultLazy()
			);
		}
	}

	public static XmlDocumentImpl consume(Binding<JaxbEntityMappingsImpl> xmlBinding, PersistenceUnitMetadata metadata) {
		final JaxbEntityMappingsImpl jaxbRoot = xmlBinding.getRoot();
		return new XmlDocumentImpl(
				xmlBinding.getOrigin(),
				xmlBinding.getRoot(),
				DefaultsImpl.consume( jaxbRoot, metadata ),
				jaxbRoot.getEntities(),
				jaxbRoot.getMappedSuperclasses(),
				jaxbRoot.getEmbeddables(),
				jaxbRoot.getConverters(),
				jaxbRoot.getConverterRegistrations(),
				jaxbRoot.getJavaTypeRegistrations(),
				jaxbRoot.getJdbcTypeRegistrations(),
				jaxbRoot.getUserTypeRegistrations(),
				jaxbRoot.getCompositeUserTypeRegistrations(),
				jaxbRoot.getCollectionUserTypeRegistrations(),
				jaxbRoot.getEmbeddableInstantiatorRegistrations(),
				toNamedQueryMap( jaxbRoot.getNamedQueries() ),
				toNamedNativeQueryMap( jaxbRoot.getNamedNativeQueries() ),
				toNamedProcedureQueryMap( jaxbRoot.getNamedProcedureQueries() ),
				// not sure what's up with the Hibernate-specific named query nodes, but they are not in the root mapping
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	private static Map<String, JaxbNamedHqlQueryImpl> toNamedQueryMap(List<JaxbNamedHqlQueryImpl> namedQueries) {
		if ( isEmpty( namedQueries ) ) {
			return Collections.emptyMap();
		}

		final Map<String, JaxbNamedHqlQueryImpl> map = new HashMap<>();
		namedQueries.forEach( (query) -> map.put( query.getName(), query ) );
		return map;
	}

	private static Map<String, JaxbNamedNativeQueryImpl> toNamedNativeQueryMap(List<JaxbNamedNativeQueryImpl> namedQueries) {
		if ( isEmpty( namedQueries ) ) {
			return Collections.emptyMap();
		}

		final Map<String, JaxbNamedNativeQueryImpl> map = new HashMap<>();
		namedQueries.forEach( (query) -> map.put( query.getName(), query ) );
		return map;
	}

	private static Map<String,JaxbNamedStoredProcedureQueryImpl> toNamedProcedureQueryMap(List<JaxbNamedStoredProcedureQueryImpl> namedQueries) {
		if ( isEmpty( namedQueries ) ) {
			return Collections.emptyMap();
		}

		final Map<String, JaxbNamedStoredProcedureQueryImpl> map = new HashMap<>();
		namedQueries.forEach( (query) -> map.put( query.getName(), query ) );
		return map;
	}
}
