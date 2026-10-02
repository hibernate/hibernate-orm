package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;

import static java.util.Collections.emptyList;

public final class EmptyAttributeMappingsMap implements AttributeMappingsMap {

	public static final EmptyAttributeMappingsMap INSTANCE = new EmptyAttributeMappingsMap();

	@Override
	public void forEachValue(@Nonnull Consumer<? super AttributeMapping> action) {
		//no-op
	}

	@Override
	public int size() {
		return 0;
	}

	@Nullable
	@Override
	public AttributeMapping get(@Nonnull String name) {
		return null;
	}

	@Nonnull
	@Override
	public Iterable<AttributeMapping> valueIterator() {
		return emptyList();
	}

}
