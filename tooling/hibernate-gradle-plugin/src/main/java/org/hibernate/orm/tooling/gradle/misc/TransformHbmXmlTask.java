/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.tooling.gradle.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.xsd.MappingXsdSupport;

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
	private final Property<TransformationNaming> renaming;
	private final Property<UnsupportedFeatureHandling> unsupportedFeatures;
	private final Property<Boolean> deleteHbmFiles;

	private final DirectoryProperty outputDirectory;

	public TransformHbmXmlTask() {
		this.outputDirectory = getProject().getObjects().directoryProperty();

		this.renaming = getProject().getObjects().property( TransformationNaming.class );
		this.renaming.set( new TransformationNaming( getProject().getObjects() ) );

		this.unsupportedFeatures = getProject().getObjects().property( UnsupportedFeatureHandling.class );
		this.unsupportedFeatures.convention( UnsupportedFeatureHandling.ERROR );

		this.deleteHbmFiles = getProject().getObjects().property( Boolean.class );
		this.deleteHbmFiles.convention( false );
	}

	/**
	 * Ability to create copies of the original with specific naming.
	 */
	@SuppressWarnings("unused")
	@Nested
	public Property<TransformationNaming> getRenaming() {
		return renaming;
	}

	/**
	 * How should features supported in `hbm.xml` files, which are not supported for transformation, be handled?
	 */
	@Input
	public Property<UnsupportedFeatureHandling> getUnsupportedFeatures() {
		return unsupportedFeatures;
	}

	/**
	 * Should the {@code hbm.xml} files be deleted on successful transformation?
	 * Default is false.
	 */
	@Input
	public Property<Boolean> getDeleteHbmFiles() {
		return deleteHbmFiles;
	}

	/**
	 * If set, transformed xml is written, relatively, to this directory.
	 * <p>
	 * E.g. transforming the resource `org/hibernate/test/my_mappings.hbm.xml`
	 * into `/home/me/hibernate` would transform the HBM mapping and save it
	 * as `/home/me/hibernate/org/hibernate/test/my_mappings.hbm.xml` (depending
	 * on {@link #getRenaming() naming} any config
	 */
	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	@TaskAction
	public void transformFiles() {
		final MappingBinder mappingBinder = new MappingBinder(
				MappingBinder.class.getClassLoader()::getResourceAsStream,
				unsupportedFeatures.getOrElse( UnsupportedFeatureHandling.ERROR )
		);

		final Marshaller marshaller;
		try {
			marshaller = mappingBinder.mappingJaxbContext().createMarshaller();
		}
		catch (JAXBException e) {
			throw new RuntimeException( "Unable to create JAXB Marshaller", e );
		}

		final List<Binding<JaxbHbmHibernateMapping>> hbmBindings = new ArrayList<>();
		getSource().forEach( (hbmXmlFile) -> {
			final Origin origin = new OriginImpl( hbmXmlFile );
			final Binding<JaxbHbmHibernateMapping> hbmBinding = bindMapping( mappingBinder, hbmXmlFile, origin );
			hbmBindings.add( hbmBinding );
		} );

		final List<Binding<JaxbEntityMappingsImpl>> transformedBindings = HbmXmlTransformer.transform(
				hbmBindings,
				unsupportedFeatures.get()
		);

		for ( int i = 0; i < hbmBindings.size(); i++ ) {
			final Binding<JaxbHbmHibernateMapping> hbmBinding = hbmBindings.get( i );
			final Binding<JaxbEntityMappingsImpl> transformedBinding = transformedBindings.get( i );

			final OriginImpl origin = (OriginImpl) hbmBinding.getOrigin();
			final File hbmXmlFile = origin.getHbmXmlFile();

			if ( deleteHbmFiles.getOrElse( false ) ) {
				final boolean deleted = hbmXmlFile.delete();
				if ( !deleted ) {
					getProject().getLogger().warn( "Unable to delete hbm.xml file `{}`", hbmXmlFile.getAbsoluteFile() );
				}
			}

			final String copyName = determineCopyName( hbmXmlFile );
			final File copyFile = determineCopyFile( copyName, hbmXmlFile );
			//noinspection ResultOfMethodCallIgnored
			copyFile.getParentFile().mkdirs();
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
			return mappingBinder.bind( fileStream, origin );
		}
		catch (IOException e) {
			getProject().getLogger()
					.warn( "Unable to open hbm.xml file `" + hbmXmlFile.getAbsolutePath() + "` for transformation", e );
			return null;
		}
	}

	private File determineCopyFile(String copyName, File hbmXmlFile) {
		if ( outputDirectory.isPresent() ) {
			return outputDirectory.get().file( copyName ).getAsFile();
		}
		else {
			return new File( hbmXmlFile.getParentFile(), copyName );
		}
	}

	private String determineCopyName(File hbmXmlFile) {
		final String hbmXmlFileName = hbmXmlFile.getName();
		if ( !renaming.isPresent() ) {
			return hbmXmlFileName;
		}

		final TransformationNaming renaming = this.renaming.get();
		if ( renaming.areNoneDefined() ) {
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

		final String prefix = renaming.getPrefix().getOrNull();
		if ( prefix != null ) {
			copyName = renaming.getPrefix().get() + copyName;
		}

		final String suffix = renaming.getSuffix().getOrNull();
		if ( suffix != null ) {
			copyName += suffix;
		}

		final String extension = renaming.getExtension().getOrNull();
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
