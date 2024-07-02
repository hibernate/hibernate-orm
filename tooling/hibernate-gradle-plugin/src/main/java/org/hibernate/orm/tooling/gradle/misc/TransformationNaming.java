/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.tooling.gradle.misc;

import java.io.Serializable;
import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * Defines how we rename a file being transformed.
 *
 * @apiNote All settings set to null indicates to use the same file
 * 		name.  Note that with no {@link TransformHbmXmlTask#getOutputDirectory()}
 * 		specified, this would mean replacing the original rather than making a
 * 		copy (effectively, {@link TransformHbmXmlTask#getDeleteHbmFiles `deleteHbmFiles=true`})
 */
public class TransformationNaming implements Serializable {
	private final Property<String> prefix;
	private final Property<String> suffix;
	private final Property<String> extension;

	@Inject
	public TransformationNaming(ObjectFactory objectFactory) {
		prefix = objectFactory.property( String.class );
		suffix = objectFactory.property( String.class );
		extension = objectFactory.property( String.class );
	}

	/**
	 * A prefix to apply to the file name.
	 * <p>
	 * E.g. given an {@code `hbm.xml`} file named {@code `my-mappings.hbm.xml`}
	 * and a configured prefix of {@code `transformed-`}, the copy file's
	 * name would be {@code `transformed-my-mappings.hbm.xml`}
	 *
	 * @see #getExtension()
	 */
	@Input
	@Optional
	public Property<String> getPrefix() {
		return prefix;
	}

	/**
	 * A suffix to apply to the file name.
	 * <p>
	 * E.g. given an {@code `hbm.xml`} file named {@code `my-mappings.hbm.xml`}
	 * and a configured suffix of {@code `-transformed`}, the copy file's
	 * name would be {@code `my-mappings-transformed.hbm.xml`}
	 *
	 * @see #getExtension()
	 */
	@Input
	@Optional
	public Property<String> getSuffix() {
		return suffix;
	}

	@Input
	@Optional
	public Property<String> getExtension() {
		return extension;
	}

	public boolean areAnyDefined() {
		return prefix.isPresent() || suffix.isPresent() || extension.isPresent();
	}

	public boolean areNoneDefined() {
		return !prefix.isPresent() && !suffix.isPresent() && !extension.isPresent();
	}
}
