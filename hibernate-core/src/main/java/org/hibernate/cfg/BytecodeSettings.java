/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.cfg;

/**
 * Settings which control the {@link org.hibernate.bytecode.spi.BytecodeProvider}
 * used for bytecode enhancement and reflection optimization.
 *
 * @author Steve Ebersole
 */
public interface BytecodeSettings {

	/**
	 * Selects a bytecode enhancement library.
	 * <p>
	 * At present only bytebuddy is supported, bytebuddy being the default since version 5.3.
	 *
	 * @settingDefault {@code "bytebuddy"}
	 * @deprecated Will be removed, Hibernate ORM will use the BytecodeProvider implementation it finds on the
	 * classpath loading it via the standard ServiceLoader mechanism. Currently, there is only a single
	 * implementation which is included in Hibernate ORM, so it's not possible to override this.
	 * See HHH-17643
	 */
	@Deprecated( forRemoval = true )
	String BYTECODE_PROVIDER = "hibernate.bytecode.provider";

	/**
	 * Enable association management feature in runtime bytecode enhancement
	 *
	 * @settingDefault {@code false}
	 */
	String ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT = "hibernate.enhancer.enableAssociationManagement";

	/**
	 * @deprecated Will be removed without replacement. See HHH-15641
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings("DeprecatedIsStillUsed")
	String ENHANCER_ENABLE_DIRTY_TRACKING = "hibernate.enhancer.enableDirtyTracking";

	/**
	 * @deprecated Will be removed without replacement. See HHH-15641
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated(forRemoval = true)
	String ENHANCER_ENABLE_LAZY_INITIALIZATION = "hibernate.enhancer.enableLazyInitialization";
}
