/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.enhance;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.bytecode.spi.BytecodeProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hibernate.bytecode.internal.BytecodeProviderInitiator.buildDefaultBytecodeProvider;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * Ant task for performing build-time enhancement of entity objects.
 *
 * Code based on from:
 * https://github.com/hibernate/hibernate-orm/blob/159bc99a36d86988b61b88ba91eec82cac044e1c/hibernate-core/src/main/java/org/hibernate/tool/enhance/EnhancementTask.java
 * https://github.com/hibernate/hibernate-orm/blob/159bc99a36d86988b61b88ba91eec82cac044e1c/tooling/hibernate-enhance-maven-plugin/src/main/java/org/hibernate/orm/tooling/maven/MavenEnhancePlugin.java
 * <pre>{@code
 * <target name="enhance" depends="compile">
 *     <taskdef name="enhance" classname="org.hibernate.tool.enhance.EnhancementTask">
 *         <classpath refid="<some-ant-path-including-hibernate-core-jar>"/>
 *         <classpath path="<your-classes-path>"/>
 *     </taskdef>
 *     <enhance base="${base}" dir="${base}" failOnError="true" enableLazyInitialization="true" enableDirtyTracking="false" enableAssociationManagement="false" enableExtendedEnhancement="false" />
 *     <enhance base="${base}" failOnError="true" enableLazyInitialization="true" enableDirtyTracking="false" enableAssociationManagement="false" enableExtendedEnhancement="false" >
 *       <fileset dir="${classes.dir}">
 *         <include name="com/acme/model/Foo.class"/>
 *         <include name="com/acme/model/Bar.class"/>
 *       </fileset>
 *     </enhance>
 * </target>
 * }</pre>
 *
 * @author Luis Barreiro
 * @author Taro App
 * @author Yanming Zhou
 * @see org.hibernate.engine.spi.Managed
 */
public class EnhancementTask extends Task {

	private List<FileSet> filesets = new ArrayList<FileSet>();
	private String base;
	private String dir;

	private boolean failOnError = true;
	private boolean enableLazyInitialization = true;
	private boolean enableDirtyTracking = true;
	private boolean enableAssociationManagement = false;
	private boolean enableExtendedEnhancement = false;
	private List<File> sourceSet = new ArrayList<>();

	public void addFileset(FileSet set) {
		this.filesets.add( set );
	}

	public void setBase(String base) {
		this.base = base;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	public void setEnableLazyInitialization(boolean enableLazyInitialization) {
		this.enableLazyInitialization = enableLazyInitialization;
	}

	public void setEnableDirtyTracking(boolean enableDirtyTracking) {
		this.enableDirtyTracking = enableDirtyTracking;
	}

	public void setEnableAssociationManagement(boolean enableAssociationManagement) {
		this.enableAssociationManagement = enableAssociationManagement;
	}

	public void setEnableExtendedEnhancement(boolean enableExtendedEnhancement) {
		this.enableExtendedEnhancement = enableExtendedEnhancement;
	}

	private boolean shouldApply() {
		return enableLazyInitialization || enableDirtyTracking || enableAssociationManagement || enableExtendedEnhancement;
	}

	@Override
	public void execute() throws BuildException {
		if ( !enableLazyInitialization ) {
			log( "The 'enableLazyInitialization' configuration is deprecated and will be removed. Set the value to 'true' to get rid of this warning", Project.MSG_WARN );
		}
		if ( !enableDirtyTracking ) {
			log( "The 'enableDirtyTracking' configuration is deprecated and will be removed. Set the value to 'true' to get rid of this warning", Project.MSG_WARN );
		}
		if ( !shouldApply() ) {
			log( "Skipping Hibernate bytecode enhancement task execution since no feature is enabled", Project.MSG_WARN );
			return;
		}

		if ( base == null ) {
			throw new BuildException( "The enhancement directory 'base' should be present" );
		}

		if ( !filesets.isEmpty() && dir != null ) {
			throw new BuildException( "Please remove the enhancement directory 'dir' if 'fileset' is using" );
		}

		if ( dir == null ) {
			for ( FileSet fileSet : filesets ) {
				Iterator<Resource> it = fileSet.iterator();
				while ( it.hasNext() ) {
					File file = new File( it.next().toString() );
					if ( file.isFile() ) {
						sourceSet.add( file );
					}
				}
			}

			if ( sourceSet.isEmpty() ) {
				log( "Skipping Hibernate enhancement task execution since there are no classes to enhance in the filesets " + filesets, Project.MSG_INFO );
				return;
			}
			log( "Starting Hibernate enhancement task for classes in filesets " + filesets, Project.MSG_INFO );
		}
		else {
			if ( !dir.startsWith( base ) ) {
				throw new BuildException( "The enhancement directory 'dir' (" + dir + ") is no subdirectory of 'base' (" + base + ")" );
			}
			// Perform a depth first search for sourceSet
			File root = new File( dir );
			if ( !root.exists() ) {
				log( "Skipping Hibernate enhancement task execution since there is no classes dir " + dir, Project.MSG_INFO );
				return;
			}
			walkDir( root );
			if ( sourceSet.isEmpty() ) {
				log( "Skipping Hibernate enhancement task execution since there are no classes to enhance on " + dir, Project.MSG_INFO );
				return;
			}
			log( "Starting Hibernate enhancement task for classes on " + dir, Project.MSG_INFO );
		}

		ClassLoader classLoader = toClassLoader( Collections.singletonList( new File( base ) ) );

		EnhancementContext enhancementContext = new DefaultEnhancementContext() {
			@Override
			public ClassLoader getLoadingClassLoader() {
				return classLoader;
			}

			@Override
			public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
				return enableAssociationManagement;
			}

			@Override
			public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
				return enableDirtyTracking;
			}

			@Override
			public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
				return enableLazyInitialization;
			}

			@Override
			public boolean isLazyLoadable(UnloadedField field) {
				return enableLazyInitialization;
			}

			@Override
			public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
				return enableExtendedEnhancement;
			}
		};

		if ( enableExtendedEnhancement ) {
			DEPRECATION_LOGGER.deprecatedSettingForRemoval("extended enhancement", "false");
		}

		if ( enableAssociationManagement ) {
			DEPRECATION_LOGGER.deprecatedSettingForRemoval( "management of bidirectional association persistent attributes", "false" );
		}

		final BytecodeProvider bytecodeProvider = buildDefaultBytecodeProvider();
		try {
			Enhancer enhancer = bytecodeProvider.getEnhancer( enhancementContext );
			for ( File file : sourceSet ) {
				discoverTypes( file, enhancer );
				log( "Successfully discovered types for class [" + file + "]", Project.MSG_INFO );
			}
			for ( File file : sourceSet ) {
				byte[] enhancedBytecode = doEnhancement( file, enhancer );
				if ( enhancedBytecode == null ) {
					continue;
				}
				writeOutEnhancedClass( enhancedBytecode, file );

				log( "Successfully enhanced class [" + file + "]", Project.MSG_INFO );
			}
		}
		finally {
			bytecodeProvider.resetCaches();
		}
	}

	private ClassLoader toClassLoader(List<File> runtimeClasspath) throws BuildException {
		List<URL> urls = new ArrayList<>();
		for ( File file : runtimeClasspath ) {
			try {
				urls.add( file.toURI().toURL() );
				log( "Adding classpath entry for classes root " + file.getAbsolutePath(), Project.MSG_DEBUG );
			}
			catch ( MalformedURLException e ) {
				String msg = "Unable to resolve classpath entry to URL: " + file.getAbsolutePath();
				if ( failOnError ) {
					throw new BuildException( msg, e );
				}
				log( msg, Project.MSG_WARN );
			}
		}

		return new URLClassLoader( urls.toArray( new URL[urls.size()] ), Enhancer.class.getClassLoader() );
	}

	private void discoverTypes(File javaClassFile, Enhancer enhancer) throws BuildException {
		try {
			String className = javaClassFile.getAbsolutePath().substring(
					base.length() + 1,
					javaClassFile.getAbsolutePath().length() - ".class".length()
			).replace( File.separatorChar, '.' );
			ByteArrayOutputStream originalBytes = new ByteArrayOutputStream();
			FileInputStream fileInputStream = new FileInputStream( javaClassFile );
			try {
				byte[] buffer = new byte[1024];
				int length;
				while ( ( length = fileInputStream.read( buffer ) ) != -1 ) {
					originalBytes.write( buffer, 0, length );
				}
			}
			finally {
				fileInputStream.close();
			}
			enhancer.discoverTypes( className, originalBytes.toByteArray() );
		}
		catch (Exception e) {
			String msg = "Unable to discover types for class: " + javaClassFile.getName();
			if ( failOnError ) {
				throw new BuildException( msg, e );
			}
			log( msg, e, Project.MSG_WARN );
		}
	}

	private byte[] doEnhancement(File javaClassFile, Enhancer enhancer) throws BuildException {
		try {
			String className = javaClassFile.getAbsolutePath().substring(
					base.length() + 1,
					javaClassFile.getAbsolutePath().length() - ".class".length()
			).replace( File.separatorChar, '.' );
			ByteArrayOutputStream originalBytes = new ByteArrayOutputStream();
			FileInputStream fileInputStream = new FileInputStream( javaClassFile );
			try {
				byte[] buffer = new byte[1024];
				int length;
				while ( ( length = fileInputStream.read( buffer ) ) != -1 ) {
					originalBytes.write( buffer, 0, length );
				}
			}
			finally {
				fileInputStream.close();
			}
			return enhancer.enhance( className, originalBytes.toByteArray() );
		}
		catch (Exception e) {
			String msg = "Unable to enhance class: " + javaClassFile.getName();
			if ( failOnError ) {
				throw new BuildException( msg, e );
			}
			log( msg, e, Project.MSG_WARN );
			return null;
		}
	}

	/**
	 * Expects a directory.
	 */
	private void walkDir(File dir) {
		walkDir(
				dir,
				new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.isFile() && pathname.getName().endsWith( ".class" );
					}
				},
				new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.isDirectory();
					}
				}
		);
	}

	private void walkDir(File dir, FileFilter classesFilter, FileFilter dirFilter) {
		File[] dirs = dir.listFiles( dirFilter );
		if ( dirs != null ) {
			for ( File dir1 : dirs ) {
				walkDir( dir1, classesFilter, dirFilter );
			}
		}
		File[] files = dir.listFiles( classesFilter );
		if ( files != null ) {
			Collections.addAll( sourceSet, files );
		}
	}

	private void writeOutEnhancedClass(byte[] enhancedBytecode, File file) throws BuildException {
		try {
			if ( file.delete() ) {
				if ( !file.createNewFile() ) {
					log( "Unable to recreate class file", Project.MSG_ERR );
				}
			}
			else {
				log( "Unable to delete class file", Project.MSG_ERR );
			}
		}
		catch ( IOException e ) {
			log( "Problem preparing class file for writing out enhancements", e, Project.MSG_WARN );
		}

		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream( file, false );
			outputStream.write( enhancedBytecode );
			outputStream.flush();
		}
		catch ( IOException e ) {
			String msg = String.format( "Error writing to enhanced class [%s] to file [%s]", file.getName(), file.getAbsolutePath() );
			if ( failOnError ) {
				throw new BuildException( msg, e );
			}
			log( msg, e, Project.MSG_WARN );
		}
		finally {
			try {
				if ( outputStream != null ) {
					outputStream.close();
				}
			}
			catch ( IOException ignore ) {
				// ignore
			}
		}
	}
}
