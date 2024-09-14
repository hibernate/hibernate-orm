/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.xml.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableInstantiatorRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJavaTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJdbcTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedHqlQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedStoredProcedureQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeRegistrationImpl;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public interface XmlDocument {
	List<JaxbEntityImpl> getEntityMappings();

	List<JaxbMappedSuperclassImpl> getMappedSuperclassMappings();

	List<JaxbEmbeddableImpl> getEmbeddableMappings();

	List<JaxbConverterImpl> getConverters();

	List<JaxbConverterRegistrationImpl> getConverterRegistrations();

	List<JaxbJavaTypeRegistrationImpl> getJavaTypeRegistrations();

	List<JaxbJdbcTypeRegistrationImpl> getJdbcTypeRegistrations();

	List<JaxbUserTypeRegistrationImpl> getUserTypeRegistrations();

	List<JaxbCompositeUserTypeRegistrationImpl> getCompositeUserTypeRegistrations();

	List<JaxbCollectionUserTypeRegistrationImpl> getCollectionUserTypeRegistrations();

	List<JaxbEmbeddableInstantiatorRegistrationImpl> getEmbeddableInstantiatorRegistrations();

	Map<String, JaxbNamedHqlQueryImpl> getJpaNamedQueries();

	Map<String, JaxbNamedNativeQueryImpl> getJpaNamedNativeQueries();

	Map<String, JaxbHbmNamedQueryType> getHibernateNamedQueries();

	Map<String, JaxbHbmNamedNativeQueryType> getHibernateNamedNativeQueries();

	Map<String, JaxbNamedStoredProcedureQueryImpl> getNamedStoredProcedureQueries();

	interface Defaults {
		String getPackage();
		AccessType getAccessType();
		String getAccessorStrategy();
		String getCatalog();
		String getSchema();
		boolean isAutoImport();
		boolean isLazinessImplied();
	}

	Defaults getDefaults();


}
