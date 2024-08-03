/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorPojoStandard;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorRecordIndirecting;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorRecordStandard;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.ReflectHelper.isRecord;

/**
 * EmbeddableRepresentationStrategy for an IdClass mapping
 */
public class IdClassRepresentationStrategy implements EmbeddableRepresentationStrategy {
	private final JavaType<?> idClassType;
	private final EmbeddableInstantiator instantiator;

	public IdClassRepresentationStrategy(
			IdClassEmbeddable idClassEmbeddable,
			boolean simplePropertyOrder,
			Supplier<String[]> attributeNamesAccess) {
		this.idClassType = idClassEmbeddable.getMappedJavaType();
		final Class<?> javaTypeClass = idClassType.getJavaTypeClass();
		if ( isRecord( javaTypeClass ) ) {
			if ( simplePropertyOrder ) {
				this.instantiator = new EmbeddableInstantiatorRecordStandard( javaTypeClass );
			}
			else {
				this.instantiator = EmbeddableInstantiatorRecordIndirecting.of(
						javaTypeClass,
						attributeNamesAccess.get()
				);
			}
		}
		else {
			this.instantiator = new EmbeddableInstantiatorPojoStandard(
					idClassType.getJavaTypeClass(),
					() -> idClassEmbeddable
			);
		}
	}

	@Override
	public EmbeddableInstantiator getInstantiator() {
		return instantiator;
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return null;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return idClassType;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		final PropertyAccessStrategy strategy = bootAttributeDescriptor.getPropertyAccessStrategy( idClassType.getJavaTypeClass() );

		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							idClassType.getTypeName(),
							bootAttributeDescriptor.getName()
					)
			);
		}

		return strategy.buildPropertyAccess(
				idClassType.getJavaTypeClass(),
				bootAttributeDescriptor.getName(),
				false
		);
	}
}
