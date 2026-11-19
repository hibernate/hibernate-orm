package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.accessor.MultiValueReader;
import org.hibernate.accessor.MultiValueWriter;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorPojoIndirecting;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorPojoStandard;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorRecordIndirecting;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorRecordStandard;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * EmbeddableRepresentationStrategy for an IdClass mapping
 */
public class IdClassRepresentationStrategy implements EmbeddableRepresentationStrategy {
	private final JavaType<?> idClassType;
	private final EmbeddableInstantiator instantiator;
	private final PropertyAccessorService propertyAccessorService;

	public IdClassRepresentationStrategy(
			@Nonnull PropertyAccessorService propertyAccessorService,
			@Nonnull IdClassEmbeddable idClassEmbeddable,
			@Nonnull Component idClassSource,
			boolean simplePropertyOrder,
			@Nonnull Supplier<String[]> attributeNamesAccess) {
		idClassType = idClassEmbeddable.getMappedJavaType();
		final var javaTypeClass = idClassType.getJavaTypeClass();
		if ( javaTypeClass.isRecord() ) {
			instantiator = simplePropertyOrder
					? new EmbeddableInstantiatorRecordStandard( javaTypeClass )
					: EmbeddableInstantiatorRecordIndirecting.of( javaTypeClass, attributeNamesAccess.get() );
		}
		else if ( idClassSource.getInstantiator() != null ) {
			idClassSource.sortProperties();
			instantiator = EmbeddableInstantiatorPojoIndirecting.of(
					idClassSource.getPropertyNames(),
					idClassSource.getInstantiator(),
					idClassSource.getInstantiatorPropertyNames()
			);
		}
		else {
			instantiator = new EmbeddableInstantiatorPojoStandard(
					idClassType.getJavaTypeClass(),
					() -> idClassEmbeddable
			);
		}
		this.propertyAccessorService = propertyAccessorService;
	}

	IdClassRepresentationStrategy(
			@Nonnull PropertyAccessorService propertyAccessorService,
			@Nonnull IdClassEmbeddable idClassEmbeddable,
			@Nonnull EmbeddableInstantiator instantiator) {
		this.idClassType = idClassEmbeddable.getMappedJavaType();
		this.instantiator = instantiator;
		this.propertyAccessorService = propertyAccessorService;
	}

	@Override
	@Nonnull
	public EmbeddableInstantiator getInstantiator() {
		return instantiator;
	}

	@Override
	@Nonnull
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Nullable
	@Override
	public MultiValueReader getMultiValueReader() {
		return null;
	}

	@Nullable
	@Override
	public MultiValueWriter getMultiValueWriter() {
		return null;
	}

	@Override
	@Nonnull
	public JavaType<?> getMappedJavaType() {
		return idClassType;
	}

	@Override
	@Nonnull
	public PropertyAccess resolvePropertyAccess(@Nonnull Property bootAttributeDescriptor) {
		final var strategy = bootAttributeDescriptor.getPropertyAccessStrategy( idClassType.getJavaTypeClass() );

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
				propertyAccessorService,
				idClassType.getJavaTypeClass(),
				bootAttributeDescriptor.getName(),
				false );
	}
}
