/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.enhance;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.cfg.Environment;

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
import java.util.List;

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
 * </target>
 * }</pre>
 *
 * @author Luis Barreiro
 * @author Taro App
 * @see org.hibernate.engine.spi.Managed
 */
public class EnhancementTask extends Task {

	private String base;
	private String dir;

	private boolean failOnError = true;
	private boolean enableLazyInitialization = false;
	private boolean enableDirtyTracking = false;
	private boolean enableAssociationManagement = false;
	private boolean enableExtendedEnhancement = false;
	private List<File> sourceSet = new ArrayList<>();

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
		if ( !shouldApply() ) {
			log( "Skipping Hibernate bytecode enhancement task execution since no feature is enabled", Project.MSG_WARN );
			return;
		}

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
			log( "Extended enhancement is enabled. Classes other than entities may be modified. You should consider access the entities using getter/setter methods and disable this property. Use at your own risk.", Project.MSG_WARN );
		}

		Enhancer enhancer = Environment.getBytecodeProvider().getEnhancer( enhancementContext );

		for ( File file : sourceSet ) {
			byte[] enhancedBytecode = doEnhancement( file, enhancer );
			if ( enhancedBytecode == null ) {
				continue;
			}
			writeOutEnhancedClass( enhancedBytecode, file );

			log( "Successfully enhanced class [" + file + "]", Project.MSG_INFO );
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
		for ( File dir1 : dirs ) {
			walkDir( dir1, classesFilter, dirFilter );
		}
		File[] files = dir.listFiles( classesFilter );
		Collections.addAll( sourceSet, files );
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
