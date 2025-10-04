/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.asciidoctor;

import org.hibernate.infra.asciidoctor.extensions.copytoclipboard.CopyToClipboardProcessor;
import org.hibernate.infra.asciidoctor.extensions.customnumbering.CustomNumberingProcessor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry;

public class HibernateAsciidoctorExtensionRegistry implements ExtensionRegistry {

	@Override
	public void register(Asciidoctor asciidoctor) {
		JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
		javaExtensionRegistry.docinfoProcessor( CopyToClipboardProcessor.class );
		javaExtensionRegistry.treeprocessor( CustomNumberingProcessor.class );
	}
}
