/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class BindingContextImpl implements BindingContext {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final GlobalRegistrations globalRegistrations;

	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final PhysicalNamingStrategy physicalNamingStrategy;
	private final SharedCacheMode sharedCacheMode;
	private final ClassmateContext classmateContext;
	private final BootstrapContext bootstrapContext;

	public BindingContextImpl(CategorizedDomainModel categorizedDomainModel, BootstrapContext bootstrapContext) {
		this(
				categorizedDomainModel.getClassDetailsRegistry(),
				categorizedDomainModel.getAnnotationDescriptorRegistry(),
				categorizedDomainModel.getGlobalRegistrations(),
				bootstrapContext.getMetadataBuildingOptions().getImplicitNamingStrategy(),
				bootstrapContext.getMetadataBuildingOptions().getPhysicalNamingStrategy(),
				bootstrapContext.getMetadataBuildingOptions().getSharedCacheMode(),
				bootstrapContext.getClassmateContext(),
				bootstrapContext
		);
	}

	public BindingContextImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			GlobalRegistrations globalRegistrations,
			ImplicitNamingStrategy implicitNamingStrategy,
			PhysicalNamingStrategy physicalNamingStrategy,
			SharedCacheMode sharedCacheMode,
			ClassmateContext classmateContext,
			BootstrapContext bootstrapContext) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
		this.implicitNamingStrategy = implicitNamingStrategy;
		this.physicalNamingStrategy = physicalNamingStrategy;
		this.bootstrapContext = bootstrapContext;
		this.globalRegistrations = globalRegistrations;
		this.classmateContext = classmateContext;
		this.sharedCacheMode = sharedCacheMode;
	}

	@Override
	public ClassDetailsRegistry getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	@Override
	public AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
		return annotationDescriptorRegistry;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public ClassmateContext getClassmateContext() {
		return classmateContext;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return implicitNamingStrategy;
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return physicalNamingStrategy;
	}
}
