/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.binder.source.annotations;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.Index;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.binder.source.MappingDefaults;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.binder.source.annotations.entity.EntityBinder;
import org.hibernate.metamodel.binder.source.internal.OverriddenMappingDefaults;
import org.hibernate.metamodel.domain.Type;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class AnnotationsMetadataProcessor implements AnnotationsBindingContext {
	private final AnnotationsBindingContext parentBindingContext;
	private final ConfiguredClass configuredClass;

	private final MappingDefaults mappingDefaults;

	private final EntityBinder entityBinder;

	public AnnotationsMetadataProcessor(
			AnnotationsBindingContext parentBindingContext,
			ConfiguredClass configuredClass) {
		this.parentBindingContext = parentBindingContext;
		this.configuredClass = configuredClass;

		String explicitSchemaName = null;
		String explicitCatalogName = null;
		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(),
				JPADotNames.TABLE
		);
		if ( tableAnnotation != null ) {
			AnnotationValue schemaValue = tableAnnotation.value( "schema" );
			explicitSchemaName = schemaValue != null ? schemaValue.asString() : null;
			AnnotationValue catalogValue = tableAnnotation.value( "catalog" );
			explicitCatalogName = catalogValue != null ? catalogValue.asString() : null;
		}
		this.mappingDefaults = new OverriddenMappingDefaults(
				parentBindingContext.getMappingDefaults(),
				null,			// packageName
				explicitSchemaName,
				explicitCatalogName,
				null,			// idColumnName
				null,			// discriminatorColumnName
				null,			// cascade
				null,			// property accessor
				null			// association laziness
		);

		this.entityBinder = new EntityBinder( configuredClass, this );
	}


	public void processMappingMetadata(List<String> processedEntityNames) {
		entityBinder.bind( processedEntityNames );
	}

	@Override
	public Index getIndex() {
		return parentBindingContext.getIndex();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return parentBindingContext.getServiceRegistry();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return parentBindingContext.getNamingStrategy();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return parentBindingContext.getMetadataImplementor();
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return parentBindingContext.locateClassByName( name );
	}

	@Override
	public Type makeJavaType(String className) {
		return parentBindingContext.makeJavaType( className );
	}

	@Override
	public Value<Class<?>> makeClassReference(String className) {
		return parentBindingContext.makeClassReference( className );
	}
}
