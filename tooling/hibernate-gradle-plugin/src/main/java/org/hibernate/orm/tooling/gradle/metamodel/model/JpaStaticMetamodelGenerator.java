/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationSpec;

public class JpaStaticMetamodelGenerator {
	public static void processMetamodel(
			PersistenceUnitInfo persistenceUnitInfo,
			JpaMetamodelGenerationSpec spec) {
		EntityManagerFactoryBuilder target = Bootstrap.getEntityManagerFactoryBuilder( persistenceUnitInfo, Collections.emptyMap() );
		try {
			new JpaStaticMetamodelGenerator( spec, target.metadata() ).process();
		}
		finally {
			target.cancel();
		}
	}

	private final JpaMetamodelGenerationSpec spec;
	private final MetadataImplementor metadata;

	private final Directory generationOutputDirectory;
	private final ObjectFactory objectFactory;

	private final Set<String> processedDomainTypeNames = new HashSet<>();

	private JpaStaticMetamodelGenerator(JpaMetamodelGenerationSpec spec, MetadataImplementor metadata) {
		this.spec = spec;
		this.metadata = metadata;
		this.generationOutputDirectory = spec.getGenerationOutputDirectory().get();
		this.objectFactory = new ObjectFactory( metadata );
	}

	private void process() {
		final Set<MappedSuperclass> mappedSuperclasses = metadata.getMappedSuperclassMappingsCopy();
		if ( mappedSuperclasses != null ) {
			mappedSuperclasses.forEach( this::handleMappedClass );
		}

		final java.util.Collection<PersistentClass> entityBindings = metadata.getEntityBindings();
		if ( entityBindings != null ) {
			entityBindings.forEach( this::handlePersistentClass );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void handleMappedClass(MappedSuperclass mappingDescriptor) {
		final MetamodelClass metamodelClass = objectFactory.metamodelClass( mappingDescriptor );
		handleManagedClass( metamodelClass, mappingDescriptor.getDeclaredPropertyIterator() );
	}

	@SuppressWarnings( "unchecked" )
	private void handlePersistentClass(PersistentClass persistentClass) {
		final MetamodelClass metamodelClass = objectFactory.metamodelClass( persistentClass );
		handleManagedClass( metamodelClass, persistentClass.getDeclaredPropertyIterator() );
	}

	private void handleManagedClass(MetamodelClass metamodelClass, Iterator<Property> propertyIterator) {
		if ( ! processedDomainTypeNames.add( metamodelClass.getDomainClassName() ) ) {
			// already processed
			return;
		}

		propertyIterator.forEachRemaining(
				property -> metamodelClass.addAttribute(
						objectFactory.attribute( property, property.getValue(), metamodelClass, this::handleEmbeddable )
				)
		);

		final String replaced = metamodelClass.getMetamodelClassName().replace( '.', '/' );
		final String metamodelClassJavaFileName = replaced + ".java";
		final RegularFile metamodelClassJavaFile = generationOutputDirectory.file( metamodelClassJavaFileName );

		final File metamodelClassJavaFileAsFile = metamodelClassJavaFile.getAsFile();
		metamodelClass.writeToFile( metamodelClassJavaFileAsFile, spec );
	}

	@SuppressWarnings( "unchecked" )
	private void handleEmbeddable(Component embeddedValueMapping) {
		final MetamodelClass metamodelClass = objectFactory.metamodelClass( embeddedValueMapping );
		handleManagedClass( metamodelClass, embeddedValueMapping.getPropertyIterator() );
	}
}
