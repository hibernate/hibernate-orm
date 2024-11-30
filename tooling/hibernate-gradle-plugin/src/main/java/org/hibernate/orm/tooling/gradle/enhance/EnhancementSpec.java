/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.enhance;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;

import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * DSL extension for configuring bytecode enhancement - available as `project.hibernateOrm.enhancement`
 */
@SuppressWarnings( { "unused", "RedundantSuppression" } )
public class EnhancementSpec {
	public static final String ENHANCE = "enhance";
	public static final String ENHANCEMENT = "enhancement";

	public static final String DSL_NAME = ENHANCEMENT;

	private final Property<Boolean> enableLazyInitialization;
	private final Property<Boolean> enableDirtyTracking;
	private final Property<Boolean> enableAssociationManagement;
	private final Property<Boolean> enableExtendedEnhancement;
	private final ListProperty<String> classNames;


	@Inject
	public EnhancementSpec(HibernateOrmSpec ormDsl, Project project) {
		final SourceSetContainer sourceSets = project.getExtensions().getByType( SourceSetContainer.class );

		enableLazyInitialization = makeProperty( project ).convention( true );
		enableDirtyTracking = makeProperty( project ).convention( true );
		enableAssociationManagement = makeProperty( project ).convention( false );
		enableExtendedEnhancement = makeProperty( project ).convention( false );
		classNames = project.getObjects().listProperty(String.class).convention(new ArrayList<>());
	}

	@SuppressWarnings( "UnstableApiUsage" )
	public static Property<Boolean> makeProperty(Project project) {
		return project.getObjects().property( Boolean.class );
	}

	/**
	 * Whether any property values indicate work to be done.
	 */
	public boolean hasAnythingToDo() {
		return enableLazyInitialization.get()
				|| enableDirtyTracking.get()
				|| enableAssociationManagement.get()
				|| enableExtendedEnhancement.get();
	}

	/**
	 * Whether lazy-initialization handling should be incorporated into the enhanced bytecode
	 */
	public Property<Boolean> getEnableLazyInitialization() {
		return enableLazyInitialization;
	}

	/**
	 * Whether dirty-tracking should be incorporated into the enhanced bytecode
	 */
	public Property<Boolean> getEnableDirtyTracking() {
		return enableDirtyTracking;
	}

	/**
	 * Whether bidirectional association-management handling should be incorporated into the enhanced bytecode
	 */
	public Property<Boolean> getEnableAssociationManagement() {
		return enableAssociationManagement;
	}

	/**
	 * Whether extended enhancement should be performed.
	 */
	public Property<Boolean> getEnableExtendedEnhancement() {
		return enableExtendedEnhancement;
	}

	/**
	 * Returns the classes on which enhancement needs to be done
	 */
	public ListProperty<String> getClassNames() {
		return classNames;
	}


	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setEnableLazyInitialization(boolean enable) {
		enableLazyInitialization.set( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void enableLazyInitialization(boolean enable) {
		setEnableLazyInitialization( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void lazyInitialization(boolean enable) {
		setEnableLazyInitialization( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setLazyInitialization(boolean enable) {
		setEnableLazyInitialization( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setEnableDirtyTracking(boolean enable) {
		enableDirtyTracking.set( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void enableDirtyTracking(boolean enable) {
		setEnableDirtyTracking( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void dirtyTracking(boolean enable) {
		setEnableDirtyTracking( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setDirtyTracking(boolean enable) {
		setEnableDirtyTracking( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setEnableAssociationManagement(boolean enable) {
		enableAssociationManagement.set( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void enableAssociationManagement(boolean enable) {
		setEnableAssociationManagement( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void associationManagement(boolean enable) {
		setEnableAssociationManagement( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setEnableExtendedEnhancement(boolean enable) {
		enableExtendedEnhancement.set( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void enableExtendedEnhancement(boolean enable) {
		setEnableExtendedEnhancement( enable );
	}

	/**
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void extendedEnhancement(boolean enable) {
		setEnableExtendedEnhancement( enable );
	}

	public void setClassNames(List<String> classNames) {
		this.classNames.set(classNames);
	}

	public void setClassNames(Provider<List<String>> classNames) {
		this.classNames.set(classNames);
	}

	public void includeClassName(String className) {
		this.classNames.add(className);
	}
}
