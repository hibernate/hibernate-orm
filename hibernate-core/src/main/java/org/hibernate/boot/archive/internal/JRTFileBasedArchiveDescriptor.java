/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import org.hibernate.boot.archive.spi.AbstractArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import org.jboss.logging.Logger;

/**
 * An ArchiveDescriptor implementation leveraging the {@link java.util.jar.JarFile} API for processing - specifically meant to support the new URL format for JRT and modules
 *
 * @author Steve Ebersole
 * @author Marc Magon
 */
public class JRTFileBasedArchiveDescriptor extends AbstractArchiveDescriptor
{
	private static final Logger log = Logger.getLogger( JRTFileBasedArchiveDescriptor.class );
	
	private ArchiveContext context;
	
	/**
	 * Constructs a JarFileBasedArchiveDescriptor
	 *
	 * @param archiveDescriptorFactory The factory creating this
	 * @param archiveUrl               The url to the JRT Module VFS file
	 * @param entry                    The prefix for entries within the JRT VFS url
	 */
	public JRTFileBasedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL archiveUrl,
			String entry)
	{
		super(archiveDescriptorFactory, archiveUrl, entry);
	}
	
	/**
	 * Visits a JRT virtual file system using the JDK 8 nio file mechanisms,
	 *
	 * @param context
	 */
	@Override
	public void visitArchive(ArchiveContext context)
	{
		this.context = context;
		java.nio.file.Path path = null;
		try
		{
			path = java.nio.file.Paths.get(getArchiveUrl().toURI());
			java.nio.file.Files.walkFileTree(path, new JRTFileBasedArchiveDescriptor.JRTModuleWalker());
		}
		catch (URISyntaxException | IOException e)
		{
			throw new IllegalArgumentException(
					"Unable to visit JRT / Path " + path + ". Cause: " + e.getMessage(), e
			);
		}
	}
	
	/**
	 * Peruses through the modular file system utilizing the <code>.nio</code> file walkers
	 * Finds anything applicable in modules that have opened access to the module <code>org.hibernate.orm.core</code>
	 *
	 * Ignores modules that have not exposed and/or opened to our module
	 */
	protected class JRTModuleWalker extends java.nio.file.SimpleFileVisitor<java.nio.file.Path>
	{
		@Override
		public java.nio.file.FileVisitResult visitFile(java.nio.file.Path path, java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException
		{
			String name = path.toString();
			if (name.startsWith("/modules/"))
			{
				name = name.substring(9);
				name = name.substring(name.indexOf('/') + 1);
			}
			java.nio.file.Path lateralPath = null;
			try
			{
				lateralPath = java.nio.file.Paths.get(getArchiveUrl().toURI());
			}
			catch (java.net.URISyntaxException e)
			{
				return visitFileFailed(path, new java.io.IOException("Unable to walk file", e));
			}
			String basePrefix = getEntryBasePrefix();
			final String entryName = name;
			final String relativeName = basePrefix != null && name.contains(basePrefix)
					? name.substring(basePrefix.length())
					: name;
			final InputStreamAccess inputStreamAccess;
			try (InputStream is = java.nio.file.Files.newInputStream(lateralPath))
			{
				inputStreamAccess = buildByteBasedInputStreamAccess(name, is);
			}
			catch (Exception e)
			{
				throw new IOException(
						String.format(
								"Unable to access stream from jrt ref [%s] for entry [%s]",
								lateralPath,
								lateralPath.toString()
						)
				);
			}
			final ArchiveEntry entry = new ArchiveEntry()
			{
				@Override
				public String getName()
				{
					return entryName;
				}
				
				@Override
				public String getNameWithinArchive()
				{
					return relativeName;
				}
				
				@Override
				public InputStreamAccess getStreamAccess()
				{
					return inputStreamAccess;
				}
			};
			
			final ArchiveEntryHandler entryHandler = context.obtainArchiveEntryHandler(entry);
			entryHandler.handleEntry(entry, context);
			return super.visitFile(lateralPath, attrs);
		}
		
		@Override
		public java.nio.file.FileVisitResult visitFileFailed(java.nio.file.Path file, java.io.IOException exc) throws java.io.IOException
		{
			return super.visitFileFailed(file, exc);
		}
	}
}
