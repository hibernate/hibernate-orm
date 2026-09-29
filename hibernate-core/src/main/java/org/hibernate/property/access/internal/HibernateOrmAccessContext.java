package org.hibernate.property.access.internal;

import org.hibernate.accessor.spi.AccessContext;
import org.hibernate.accessor.spi.AccessorConfiguration;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.util.Map;

final class HibernateOrmAccessContext implements AccessContext {

	static AccessorConfiguration configuration(Map<String, Object> configurationValues) {
		return new AccessorConfiguration( new HibernateOrmAccessContext(), configurationValues );
	}

	private final MethodHandles.Lookup lookup;

	private HibernateOrmAccessContext() {
		this.lookup = MethodHandles.lookup();
	}

	@Override
	public MethodHandles.Lookup lookup() {
		return lookup;
	}

	@Override
	public void ensureReads(Module target) {
		lookup.lookupClass().getModule().addReads( target );
	}

	@Override
	public void makeAccessible(AccessibleObject member) {
		member.setAccessible( true );
	}
}
