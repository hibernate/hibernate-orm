/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.internal.RepeatableInputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Database;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

/**
 * Task to transform a legacy {@code `hbm.xml`} file into the
 * {@link MappingXsdSupport#latestDescriptor() mapping} format.
 * <p>
 * The task creates copies of {@code `hbm.xml`} files:<ul>
 * <li>
 * Into the specified {@link #getOutputDirectory() output directory},
 * if one was set; otherwise into the same directory as the
 * {@code `hbm.xml`} file.
 * </li>
 * <li>
 * Use {@link #getRenaming() renaming} to control the naming of the
 * copies.
 * </li>
 * </ul>
 *
 * @author Steve Ebersole
 * @apiNote See note on {@link TransformationNaming} with regard to
 * 		making copies in place and not specifying renaming effectively
 * 		replacing the original (destructive).
 * @see org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer
 */
@CacheableTask
public abstract class TransformHbmXmlTask extends SourceTask {

	public TransformHbmXmlTask() {
		getTargetDatabaseName().convention( "H2" );
		getUnsupportedFeatures().convention( UnsupportedFeatureHandling.ERROR );
		getDeleteHbmFiles().convention( false );
	}

	/**
	 * Ability to create copies of the original with specific naming.
	 */
	@Nested
	abstract public TransformationNaming getRenaming();

	/**
	 * @see Database
	 */
	@Input
	abstract public Property<String> getTargetDatabaseName();

	/**
	 * How should features supported in `hbm.xml` files, which are not supported for transformation, be handled?
	 */
	@Input
	abstract public Property<UnsupportedFeatureHandling> getUnsupportedFeatures();

	/**
	 * Should the {@code hbm.xml} files be deleted on successful transformation?
	 * Default is false.
	 */
	@Input
	abstract public Property<Boolean> getDeleteHbmFiles();

	/**
	 * If set, transformed xml is written, relatively, to this directory.
	 * <p>
	 * E.g. transforming the resource `org/hibernate/test/my_mappings.hbm.xml`
	 * into `/home/me/hibernate` would transform the HBM mapping and save it
	 * as `/home/me/hibernate/org/hibernate/test/my_mappings.hbm.xml` (depending
	 * on {@link #getRenaming() naming} config)
	 */
	@OutputDirectory
	abstract public DirectoryProperty getOutputDirectory();

	@SuppressWarnings("unused")
	public void renaming(Action<TransformationNaming> action) {
		action.execute( getRenaming() );
	}


	@TaskAction
	public void transformFiles() {
		final MappingBinder mappingBinder = new MappingBinder(
				MappingBinder.class.getClassLoader()::getResourceAsStream,
				getUnsupportedFeatures().get()
		);

		final List<Binding<JaxbHbmHibernateMapping>> hbmBindings = new ArrayList<>();
		getSource().forEach( (hbmXmlFile) -> {
			final Origin origin = new OriginImpl( hbmXmlFile );
			final Binding<JaxbHbmHibernateMapping> hbmBinding = bindMapping( mappingBinder, hbmXmlFile, origin );
			hbmBindings.add( hbmBinding );
		} );

		try ( StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.clearSettings()
				.applySetting( JdbcSettings.JAKARTA_HBM2DDL_DB_NAME, getTargetDatabaseName().get() )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.build() ) {
			performTransformation( hbmBindings, mappingBinder, serviceRegistry );
		}
	}

	private void performTransformation(
			List<Binding<JaxbHbmHibernateMapping>> hbmBindings,
			MappingBinder mappingBinder, StandardServiceRegistry serviceRegistry) {
		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		hbmBindings.forEach( metadataSources::addHbmXmlBinding );

		final List<Binding<JaxbEntityMappingsImpl>> transformedBindings = HbmXmlTransformer.transform(
				hbmBindings,
				(MetadataImplementor) metadataSources.buildMetadata(),
				getUnsupportedFeatures().get()
		);

		for ( int i = 0; i < hbmBindings.size(); i++ ) {
			final Binding<JaxbHbmHibernateMapping> hbmBinding = hbmBindings.get( i );
			final Binding<JaxbEntityMappingsImpl> transformedBinding = transformedBindings.get( i );

			final OriginImpl origin = (OriginImpl) hbmBinding.getOrigin();
			final File hbmXmlFile = origin.getHbmXmlFile();

			if ( getDeleteHbmFiles().get() ) {
				final boolean deleted = hbmXmlFile.delete();
				if ( !deleted ) {
					getProject().getLogger().warn( "Unable to delete hbm.xml file `{}`", hbmXmlFile.getAbsoluteFile() );
				}
			}

			final String copyName = determineCopyName( hbmXmlFile );
			final File copyFile = determineCopyFile( copyName, hbmXmlFile );
			//noinspection ResultOfMethodCallIgnored
			copyFile.getParentFile().mkdirs();


			final Marshaller marshaller;
			try {
				marshaller = mappingBinder.mappingJaxbContext().createMarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException( "Unable to create JAXB Marshaller", e );
			}

			try {
				marshaller.marshal( transformedBinding.getRoot(), copyFile );
			}
			catch (JAXBException e) {
				throw new RuntimeException(
						"Unable to marshall mapping JAXB representation to file `" + copyFile.getAbsolutePath() + "`",
						e
				);
			}
		}
	}

	private Binding<JaxbHbmHibernateMapping> bindMapping(MappingBinder mappingBinder, File hbmXmlFile, Origin origin) {
		try ( final FileInputStream fileStream = new FileInputStream( hbmXmlFile ) ) {
			return mappingBinder.bind( new RepeatableInputStreamAccess( hbmXmlFile.getPath(), fileStream), origin );
		}
		catch (IOException e) {
			getProject().getLogger()
					.warn( "Unable to open hbm.xml file `" + hbmXmlFile.getAbsolutePath() + "` for transformation", e );
			return null;
		}
	}

	private File determineCopyFile(String copyName, File hbmXmlFile) {
		if ( getOutputDirectory().isPresent() ) {
			return getOutputDirectory().get().file( copyName ).getAsFile();
		}
		else {
			return new File( hbmXmlFile.getParentFile(), copyName );
		}
	}

	private String determineCopyName(File hbmXmlFile) {
		final String hbmXmlFileName = hbmXmlFile.getName();
		if ( getRenaming().areNoneDefined() ) {
			return hbmXmlFileName;
		}

		final String copyBaseName;
		final String hbmXmlFileExtension;
		final int legacyConventionExtensionIndex = hbmXmlFileName.indexOf( ".hbm.xml" );
		if ( legacyConventionExtensionIndex > 0 ) {
			copyBaseName = hbmXmlFileName.substring( 0, legacyConventionExtensionIndex );
			hbmXmlFileExtension = ".hbm.xml";
		}
		else {
			final int extensionIndex = hbmXmlFileName.lastIndexOf( ".'" );
			if ( extensionIndex > 0 ) {
				copyBaseName = hbmXmlFileName.substring( 0, legacyConventionExtensionIndex );
				hbmXmlFileExtension = hbmXmlFileName.substring( extensionIndex + 1 );
			}
			else {
				copyBaseName = hbmXmlFileName;
				hbmXmlFileExtension = null;
			}
		}

		String copyName = copyBaseName;

		final String prefix = getRenaming().getPrefix().getOrNull();
		if ( prefix != null ) {
			copyName = getRenaming().getPrefix().get() + copyName;
		}

		final String suffix = getRenaming().getSuffix().getOrNull();
		if ( suffix != null ) {
			copyName += suffix;
		}

		final String extension = getRenaming().getExtension().getOrNull();
		if ( extension != null ) {
			copyName += ".";
			copyName += extension;
		}
		else if ( hbmXmlFileExtension != null ) {
			copyName += ".";
			copyName += hbmXmlFileExtension;
		}

		return copyName;
	}
}
