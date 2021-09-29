package org.hibernate.boot.archive.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.hibernate.boot.archive.spi.*;

import static org.hibernate.internal.log.UrlMessageBundle.URL_LOGGER;

/**
 * An ArchiveDescriptor implementation that works on archives accessible through a {@link JarInputStream}.
 * NOTE : This is less efficient implementation than {@link JarFileBasedArchiveDescriptor}
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JRTInputStreamBasedArchiveDescriptor
		extends AbstractArchiveDescriptor
{

	/**
	 * Constructs a JarInputStreamBasedArchiveDescriptor
	 *
	 * @param archiveDescriptorFactory
	 * 		The factory creating this
	 * @param url
	 * 		The url to the JRT VFS URL
	 * @param entry
	 * 		The prefix for entries within the JAR VFS url
	 */
	public JRTInputStreamBasedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL url,
			String entry)
	{
		super(archiveDescriptorFactory, url, entry);
	}

	@Override
	public void visitArchive(ArchiveContext context)
	{
		final String jrtPart = getArchiveUrl().getPath();
		if (getArchiveUrl().toString()
		                   .startsWith("jrt:/"))
		{
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
							throw new RuntimeException(
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
			return;
		}
		else {
			throw new ArchiveException(
					String.format(
							"JRT reader not meant for url location [%s]",
							jrtPart)
			);
		}
	}
}
