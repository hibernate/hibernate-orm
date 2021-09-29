/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import org.hibernate.boot.archive.spi.*;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import static org.hibernate.internal.log.UrlMessageBundle.*;

/**
 * An ArchiveDescriptor implementation leveraging the {@link java.util.jar.JarFile} API for processing - specifically meant to support the new URL format for JRT and modules
 *
 * @author Steve Ebersole
 * @author Marc Magon
 */
public class JRTFileBasedArchiveDescriptor extends AbstractArchiveDescriptor {
	/**
	 * Constructs a JarFileBasedArchiveDescriptor
	 *
	 * @param archiveDescriptorFactory The factory creating this
	 * @param archiveUrl The url to the JRT Module VFS file
	 * @param entry The prefix for entries within the JRT VFS url
	 */
	public JRTFileBasedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL archiveUrl,
			String entry) {
		super( archiveDescriptorFactory, archiveUrl, entry );
	}

	@Override
	public void visitArchive(ArchiveContext context) {
		final String jrtPart = getArchiveUrl().getPath();
		try
		{
			Path path = Paths.get(getArchiveUrl().toURI());
			String basePrefix = getEntryBasePrefix();
			String name = path.toString();
			name = name.startsWith("/modules/") ? name.substring(9) : name;
			Files.walkFileTree(path, new SimpleFileVisitor<>(){
				@Override
				public FileVisitResult visitFile(Path a, BasicFileAttributes attrs) throws IOException
				{
					String name = a.toString();
					if(name.startsWith("/modules/"))
					{
						name = name.substring(9);
						name = name.substring(name.indexOf('/') + 1);
					}
					
					final String entryName = name;
					final String relativeName = basePrefix != null && name.contains(basePrefix)
							? name.substring(basePrefix.length())
							: name;
					final InputStreamAccess inputStreamAccess;
					try (InputStream is = Files.newInputStream(a))
					{
						inputStreamAccess = buildByteBasedInputStreamAccess(name, is);
					}
					catch (Exception e)
					{
						System.out.println("exception in walk - ");
						e.printStackTrace();
						throw new ArchiveException(
								String.format(
										"Unable to access stream from jrt ref [%s] for entry [%s]",
										path,
										a.toString()
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
					return super.visitFile(a, attrs);
				}
			});
		}
		catch (URISyntaxException | IOException e)
		{
			throw new ArchiveException(
					String.format(
							"Unable to access stream from jrt location [%s]",
							jrtPart)
			);
		}
	}

	private JarFile resolveJarFileReference() {
		try {
			final String filePart = getArchiveUrl().getFile();
			if ( filePart != null && filePart.indexOf( ' ' ) != -1 ) {
				// unescaped (from the container), keep as is
				return new JarFile( getArchiveUrl().getFile() );
			}
			else {
				return new JarFile( getArchiveUrl().toURI().getSchemeSpecificPart() );
			}
		}
		catch (IOException e) {
			URL_LOGGER.logUnableToFindFileByUrl( getArchiveUrl(), e );
		}
		catch (URISyntaxException e) {
			URL_LOGGER.logMalformedUrl( getArchiveUrl(), e );
		}
		return null;
	}
}
