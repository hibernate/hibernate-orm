/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Maven mojo for performing build-time enhancement of entity objects.
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
		requiresDependencyResolution = ResolutionScope.COMPILE)
public class HibernateEnhancerMojo extends AbstractMojo {

	final private List<File> sourceSet = new ArrayList<File>();
	private Enhancer enhancer;

	/**
	 * A list of FileSets in which to look for classes to enhance.
	 * This parameter is optional but if it is specified, the 'classesDirectory' parameter is ignored.
	 */
	@Parameter
	private FileSet[] fileSets;

	/**
	 * The folder in which to look for classes to enhance.
	 * This parameter is required but if the 'fileSets' parameter is specified, it will be ignored.
	 */
	@Parameter(
			defaultValue = "${project.build.directory}/classes",
			required = true)
	private File classesDirectory;

	/**
	 * A boolean that indicates whether or not to add association management to automatically
	 * synchronize a bidirectional association when only one side is changed
	 */
	@Parameter(
			defaultValue = "false",
			required = true)
	private boolean enableAssociationManagement;

	/**
	 * A boolean that indicates whether or not to add dirty tracking
	 * @deprecated <a href="https://hibernate.atlassian.net/browse/HHH-15641">See HHH-15641</a>
	 */
	@Deprecated(
			forRemoval = true)
	@Parameter(
			defaultValue = "true",
			required = true)
	private boolean enableDirtyTracking;

	/**
	 * A boolean that indicates whether or not to add lazy initialization
	 * @deprecated <a href="https://hibernate.atlassian.net/browse/HHH-15641">See HHH-15641</a>
	 */
	@Deprecated(
			forRemoval = true)
	@Parameter(
			defaultValue = "true",
			required = true)
	private boolean enableLazyInitialization;

	/**
	 * A boolean that indicates whether or not to add extended enhancement.
	 * This setting will provide bytecode enhancement, even for non-entity classes
	 */
	@Parameter(
			defaultValue = "false",
			required = true)
	private boolean enableExtendedEnhancement;

	/**
	 * The Maven Project Object
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	public void execute() throws MojoExecutionException {
		getLog().debug(STARTING_EXECUTION_OF_ENHANCE_MOJO);
		processParameters();
		if (enhancementIsNeeded()) {
			assembleSourceSet();
			createEnhancer();
			discoverTypes();
			performEnhancement();
		}
		getLog().debug(ENDING_EXECUTION_OF_ENHANCE_MOJO);
	}

	private boolean enhancementIsNeeded() {
		// enhancement is not needed when all the parameters are false
		return enableAssociationManagement ||
			enableDirtyTracking ||
			enableLazyInitialization ||
			enableExtendedEnhancement;
	}

	private void processParameters() {
		if (!enableLazyInitialization) {
			getLog().warn(ENABLE_LAZY_INITIALIZATION_DEPRECATED);
		}
		if (!enableDirtyTracking) {
			getLog().warn(ENABLE_DIRTY_TRACKING_DEPRECATED);
		}
		if (fileSets == null) {
			fileSets = new FileSet[1];
			fileSets[0] = new FileSet();
			fileSets[0].setDirectory(classesDirectory.getAbsolutePath());
			getLog().debug(ADDED_DEFAULT_FILESET_WITH_BASE_DIRECTORY.formatted(fileSets[0].getDirectory()));
		}
	}

	private void assembleSourceSet() {
		getLog().debug(STARTING_ASSEMBLY_OF_SOURCESET);
		for (FileSet fileSet : fileSets) {
			addFileSetToSourceSet(fileSet);
		}
		getLog().debug(ENDING_ASSEMBLY_OF_SOURCESET);
	}

	private void addFileSetToSourceSet(FileSet fileSet) {
		getLog().debug(PROCESSING_FILE_SET);
		String directory = fileSet.getDirectory();
		FileSetManager fileSetManager = new FileSetManager();
		File baseDir = classesDirectory;
		if (directory != null && classesDirectory != null) {
			baseDir = new File(directory);
		}
		getLog().debug(USING_BASE_DIRECTORY.formatted(baseDir));
		for (String fileName : fileSetManager.getIncludedFiles(fileSet)) {
			File candidateFile = new File(baseDir, fileName);
			if (fileName.endsWith(".class")) {
				sourceSet.add(candidateFile);
				getLog().info(ADDED_FILE_TO_SOURCE_SET.formatted(candidateFile));
			}
			else {
				getLog().debug(SKIPPING_NON_CLASS_FILE.formatted(candidateFile));
			}
		}
		getLog().debug(FILESET_PROCESSED_SUCCESSFULLY);
	}

	private ClassLoader createClassLoader() throws MojoExecutionException {
		getLog().debug(CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory)) ;
		List<URL> urls = new ArrayList<>();
		try {
			urls.add(classesDirectory.toURI().toURL());
		}
		catch (MalformedURLException e) {
			throw new MojoExecutionException(UNEXPECTED_ERROR_WHILE_CONSTRUCTING_CLASSLOADER, e);
		}

		// Add dependencies to classpath as well - all but the ones used for testing purposes
		final Set<Artifact> artifacts = this.project.getArtifacts();
		if (artifacts != null) {
			boolean success = true;
			for (var artifact : artifacts) {
				if ( !Artifact.SCOPE_TEST.equals(artifact.getScope() ) ) {
					try {
						urls.add(artifact.getFile().toURI().toURL() );
						getLog().debug(CREATE_URL_CLASSLOADER_ADD_DEPENDENCY_ARTIFACT.formatted(artifact.getId()));
					}
					catch (MalformedURLException e) {
						success = false;
						getLog().error(UNEXPECTED_ERROR_WHILE_CONSTRUCTING_CLASSLOADER_ADD_DEPENDENCY_ARTIFACT
								.formatted(artifact.getId(), artifact.getFile().getAbsolutePath()), e);
					}
				}
			}
			if (!success) {
				throw new MojoExecutionException(UNEXPECTED_ERROR_WHILE_CONSTRUCTING_CLASSLOADER_DEPENDENCIES);
			}
		}

		return new URLClassLoader(
				urls.toArray(new URL[0]),
				Enhancer.class.getClassLoader());
	}

	private EnhancementContext createEnhancementContext() throws MojoExecutionException {
		getLog().debug(CREATE_ENHANCEMENT_CONTEXT) ;
		return new EnhancementContext(
				createClassLoader(),
				enableAssociationManagement,
				enableDirtyTracking,
				enableLazyInitialization,
				enableExtendedEnhancement);
	}

	private void createEnhancer() throws MojoExecutionException {
		getLog().debug(CREATE_BYTECODE_ENHANCER) ;
		enhancer = BytecodeProviderInitiator
				.buildDefaultBytecodeProvider()
				.getEnhancer(createEnhancementContext());
	}

	private void discoverTypes() throws MojoExecutionException {
		getLog().debug(STARTING_TYPE_DISCOVERY) ;
		for (File classFile : sourceSet) {
			discoverTypesForClass(classFile);
		}
		getLog().debug(ENDING_TYPE_DISCOVERY) ;
	}

	private void discoverTypesForClass(File classFile) throws MojoExecutionException {
		getLog().debug(TRYING_TO_DISCOVER_TYPES_FOR_CLASS_FILE.formatted(classFile));
		try {
			enhancer.discoverTypes(
					determineClassName(classFile),
					Files.readAllBytes(classFile.toPath()));
			getLog().info(SUCCESSFULLY_DISCOVERED_TYPES_FOR_CLASS_FILE.formatted(classFile));
		}
		catch (IOException e) {
			throw new MojoExecutionException(UNABLE_TO_DISCOVER_TYPES_FOR_CLASS_FILE.formatted(classFile), e);
		}
	}

	private String determineClassName(File classFile) {
		getLog().debug(DETERMINE_CLASS_NAME_FOR_FILE.formatted(classFile));
		String classFilePath = classFile.getAbsolutePath();
		String classesDirectoryPath = classesDirectory.getAbsolutePath();
		return classFilePath.substring(
						classesDirectoryPath.length() + 1,
						classFilePath.length() - ".class".length())
				.replace(File.separatorChar, '.');
	}

	private void performEnhancement() throws MojoExecutionException {
		getLog().debug(STARTING_CLASS_ENHANCEMENT) ;
		boolean success = true;
		for (File classFile : sourceSet) {
			long lastModified = classFile.lastModified();
			success = enhanceClass(classFile) & success;
			final boolean timestampReset = classFile.setLastModified( lastModified );
			if ( !timestampReset ) {
				getLog().debug(SETTING_LASTMODIFIED_FAILED_FOR_CLASS_FILE.formatted(classFile));
			}
		}
		if (!success) {
			throw new MojoExecutionException(ERROR_WHILE_ENHANCING_CLASSES);
		}
		getLog().debug(ENDING_CLASS_ENHANCEMENT) ;
	}

	private boolean enhanceClass(File classFile) throws MojoExecutionException {
		getLog().debug(TRYING_TO_ENHANCE_CLASS_FILE.formatted(classFile));
		try {
			byte[] newBytes = enhancer.enhance(
					determineClassName(classFile),
					Files.readAllBytes(classFile.toPath()));
			if (newBytes != null) {
				writeByteCodeToFile(newBytes, classFile);
				getLog().info(SUCCESSFULLY_ENHANCED_CLASS_FILE.formatted(classFile));
			}
			else {
				getLog().info(SKIPPING_FILE.formatted(classFile));
			}
		}
		catch (EnhancementException | IOException e) {
			getLog().error(ERROR_WHILE_ENHANCING_CLASS_FILE.formatted(classFile), e);
			return false;
		}
		return true;
	}

	private void writeByteCodeToFile(byte[] bytes, File file) throws MojoExecutionException {
		getLog().debug(WRITING_BYTE_CODE_TO_FILE.formatted(file));
		if (clearFile(file)) {
			try {
				Files.write(file.toPath(), bytes);
				getLog().debug(AMOUNT_BYTES_WRITTEN_TO_FILE.formatted(bytes.length, file));
			}
			catch (FileNotFoundException e) {
				throw new MojoExecutionException(ERROR_OPENING_FILE_FOR_WRITING.formatted(file), e);
			}
			catch (IOException e) {
				throw new MojoExecutionException(ERROR_WRITING_BYTES_TO_FILE.formatted(file), e);
			}
		}
		else {
			throw new MojoExecutionException(UNABLE_TO_DELETE_FILE.formatted(file));
		}
	}

	private boolean clearFile(File file) throws MojoExecutionException {
		getLog().debug(TRYING_TO_CLEAR_FILE.formatted(file));
		boolean success = false;
		if ( file.delete() ) {
			try {
				if ( !file.createNewFile() ) {
					throw new MojoExecutionException(UNABLE_TO_CREATE_FILE.formatted(file));
				}
				else {
					getLog().info(SUCCESSFULLY_CLEARED_FILE.formatted(file));
					success = true;
				}
			}
			catch (IOException e) {
				getLog().warn(PROBLEM_CLEARING_FILE.formatted(file), e);
			}
		}
		return success;
	}

	// info messages
	static final String SUCCESSFULLY_CLEARED_FILE = "Successfully cleared the contents of file: %s";
	static final String SUCCESSFULLY_ENHANCED_CLASS_FILE = "Successfully enhanced class file: %s";
	static final String SKIPPING_FILE = "Skipping file: %s";
	static final String SUCCESSFULLY_DISCOVERED_TYPES_FOR_CLASS_FILE = "Successfully discovered types for classes in file: %s";
	static final String ADDED_FILE_TO_SOURCE_SET = "Added file to source set: %s";

	// warning messages
	static final String PROBLEM_CLEARING_FILE = "Problem clearing file for writing out enhancements [ %s ]";
	static final String ENABLE_LAZY_INITIALIZATION_DEPRECATED = "The 'enableLazyInitialization' configuration is deprecated and will be removed. Set the value to 'true' to get rid of this warning";
	static final String ENABLE_DIRTY_TRACKING_DEPRECATED = "The 'enableDirtyTracking' configuration is deprecated and will be removed. Set the value to 'true' to get rid of this warning";

	// error messages
	static final String UNABLE_TO_CREATE_FILE = "Unable to create file: %s";
	static final String UNABLE_TO_DELETE_FILE = "Unable to delete file: %s";
	static final String ERROR_WRITING_BYTES_TO_FILE = "Error writing bytes to file : %s";
	static final String ERROR_OPENING_FILE_FOR_WRITING = "Error opening file for writing : %s";
	static final String ERROR_WHILE_ENHANCING_CLASS_FILE = "An exception occurred while trying to enhance the class file: %s";
	static final String ERROR_WHILE_ENHANCING_CLASSES = "An exception occurred while trying to enhance class file. See above logs for more details.";
	static final String UNABLE_TO_DISCOVER_TYPES_FOR_CLASS_FILE = "Unable to discover types for classes in file: %s";
	static final String UNEXPECTED_ERROR_WHILE_CONSTRUCTING_CLASSLOADER = "An unexpected error occurred while constructing the classloader";
	static final String UNEXPECTED_ERROR_WHILE_CONSTRUCTING_CLASSLOADER_ADD_DEPENDENCY_ARTIFACT = "Unable to resolve URL for dependency %s at %s";
	static final String UNEXPECTED_ERROR_WHILE_CONSTRUCTING_CLASSLOADER_DEPENDENCIES = "An unexpected error occurred while constructing the classloader. See above logs for more details.";

	// debug messages
	static final String TRYING_TO_CLEAR_FILE = "Trying to clear the contents of file: %s";
	static final String AMOUNT_BYTES_WRITTEN_TO_FILE = "%s bytes were succesfully written to file: %s";
	static final String WRITING_BYTE_CODE_TO_FILE = "Writing byte code to file: %s";
	static final String DETERMINE_CLASS_NAME_FOR_FILE = "Determining class name for file: %s";
	static final String TRYING_TO_ENHANCE_CLASS_FILE = "Trying to enhance class file: %s";
	static final String STARTING_CLASS_ENHANCEMENT = "Starting class enhancement";
	static final String SETTING_LASTMODIFIED_FAILED_FOR_CLASS_FILE = "Setting lastModified failed for class file: %s";
	static final String ENDING_CLASS_ENHANCEMENT = "Ending class enhancement";
	static final String TRYING_TO_DISCOVER_TYPES_FOR_CLASS_FILE = "Trying to discover types for classes in file: %s";
	static final String STARTING_TYPE_DISCOVERY = "Starting type discovery";
	static final String ENDING_TYPE_DISCOVERY = "Ending type discovery";
	static final String CREATE_BYTECODE_ENHANCER = "Creating bytecode enhancer";
	static final String CREATE_ENHANCEMENT_CONTEXT = "Creating enhancement context";
	static final String CREATE_URL_CLASSLOADER_FOR_FOLDER = "Creating URL ClassLoader for folder: %s";
	static final String PROCESSING_FILE_SET = "Processing FileSet";
	static final String USING_BASE_DIRECTORY = "Using base directory: %s";
	static final String SKIPPING_NON_CLASS_FILE = "Skipping non '.class' file: %s";
	static final String FILESET_PROCESSED_SUCCESSFULLY = "FileSet was processed successfully";
	static final String STARTING_ASSEMBLY_OF_SOURCESET = "Starting assembly of the source set";
	static final String ENDING_ASSEMBLY_OF_SOURCESET = "Ending the assembly of the source set";
	static final String ADDED_DEFAULT_FILESET_WITH_BASE_DIRECTORY = "Added a default FileSet with base directory: %s";
	static final String STARTING_EXECUTION_OF_ENHANCE_MOJO = "Starting execution of enhance mojo";
	static final String ENDING_EXECUTION_OF_ENHANCE_MOJO = "Ending execution of enhance mojo";
	static final String CREATE_URL_CLASSLOADER_ADD_DEPENDENCY_ARTIFACT = "Adding classpath entry for dependency %s";
}
