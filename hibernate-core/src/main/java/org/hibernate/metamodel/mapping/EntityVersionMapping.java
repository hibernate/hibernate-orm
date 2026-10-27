package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.VersionValue;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.type.descriptor.java.VersionJavaType;

/**
 * Describes the mapping of an entity's version
 *
 * @see jakarta.persistence.Version
 */
public interface EntityVersionMapping<J> extends BasicValuedModelPart<J, J, J> {
	@Override
	JdbcMapping<J> getJdbcMapping();


	String VERSION_ROLE_NAME = "{version}";

	static boolean matchesRoleName(@Nonnull String name) {
		return VERSION_ROLE_NAME.equals( name );
	}

	/**
	 * The attribute marked as the version
	 */
	@Nonnull
	@org.hibernate.Internal
	BasicAttributeMapping<J> getVersionAttribute();

	/**
	 * The strategy for distinguishing between detached and transient
	 * state based on the version mapping.
	 *
	 * @see EntityIdentifierMapping#getUnsavedStrategy()
	 */
	@Nonnull
	VersionValue getUnsavedStrategy();

	@Nonnull
	@Override
	VersionJavaType<J> getJavaType();

	@Nonnull
	@Override
	default VersionJavaType<J> getExpressibleJavaType() {
		return getJavaType();
	}

	@Nonnull
	@Override
	default AttributeMapping<J,J,J> asAttributeMapping() {
		return getVersionAttribute();
	}
}
