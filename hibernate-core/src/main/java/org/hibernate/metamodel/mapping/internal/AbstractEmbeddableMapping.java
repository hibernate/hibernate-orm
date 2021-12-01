/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Base support for EmbeddableMappingType implementations
 */
public abstract class AbstractEmbeddableMapping implements EmbeddableMappingType {
	protected final SessionFactoryImplementor sessionFactory;

	public AbstractEmbeddableMapping(MappingModelCreationProcess creationProcess) {
		this( creationProcess.getCreationContext() );
	}

	public AbstractEmbeddableMapping(RuntimeModelCreationContext creationContext) {
		this( creationContext.getSessionFactory() );
	}

	protected AbstractEmbeddableMapping(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public JavaType<?> getMappedJavaTypeDescriptor() {
		return getRepresentationStrategy().getMappedJavaTypeDescriptor();
	}

	@Override
	public Object[] getValues(Object compositeInstance) {
		if ( compositeInstance == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
			return new Object[getNumberOfAttributeMappings()];
		}

		final ReflectionOptimizer optimizer = getRepresentationStrategy().getReflectionOptimizer();
		if ( optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return optimizer.getAccessOptimizer().getPropertyValues( compositeInstance );
		}

		final Object[] results = new Object[getNumberOfAttributeMappings()];
		forEachAttributeMapping( (position, attribute) -> {
			final Getter getter = attribute.getAttributeMetadataAccess()
					.resolveAttributeMetadata( null )
					.getPropertyAccess()
					.getGetter();
			results[position] = getter.get( compositeInstance );
		} );
		return results;
	}

	@Override
	public void setValues(Object component, Object[] values) {
		final ReflectionOptimizer optimizer = getRepresentationStrategy().getReflectionOptimizer();
		if ( optimizer != null && optimizer.getAccessOptimizer() != null ) {
			optimizer.getAccessOptimizer().setPropertyValues( component, values );
		}
		else {
			forEachAttributeMapping( (position, attribute) -> {
				attribute.getPropertyAccess().getSetter().set( component, values[position] );
			} );
		}
	}
}
