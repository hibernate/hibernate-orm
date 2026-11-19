package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.accessor.Instantiator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Support for instantiating entity values as POJO representation using
 * bytecode optimizer
 *
 * @author Steve Ebersole
 */
public class EntityInstantiatorPojoOptimized extends AbstractEntityInstantiatorPojo {
	private final Instantiator<?> instantiator;

	public EntityInstantiatorPojoOptimized(
			@Nonnull EntityPersister persister,
			@Nonnull PersistentClass persistentClass,
			@Nonnull JavaType<?> javaType,
			@Nonnull Instantiator<?> instantiator) {
		super( persister, persistentClass, javaType );
		this.instantiator = instantiator;
	}

	@Override
	@Nonnull
	public Object instantiate() {
		return applyInterception( instantiator.create() );
	}
}
