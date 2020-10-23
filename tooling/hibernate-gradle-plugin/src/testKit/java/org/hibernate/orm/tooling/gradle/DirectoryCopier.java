/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

/**
 * @author Steve Ebersole
 */
public class DirectoryCopier implements FileVisitor<Path> {
	public static void copy(Path source, Path target) {
		try {
			Files.walkFileTree( source, new DirectoryCopier( source, target ) );
		}
		catch (IOException e) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Unable to copy `%s` to `%s` : %s",
							source,
							target,
							e.getMessage()
					),
					e
			);
		}
	}

	private final Path sourceBase;
	private final Path targetBase;

	public DirectoryCopier(Path sourceBase, Path targetBase) {
		this.sourceBase = sourceBase;
		this.targetBase = targetBase;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
		final Path target = targetBase.resolve( sourceBase.relativize( dir ) );

		try {
			// simply copies the structure, not files...
			Files.copy( dir, target, COPY_ATTRIBUTES );
		}
		catch (FileAlreadyExistsException x) {
			// ignore
		}
		catch (IOException x) {
			throw new IllegalStateException(
					String.format( Locale.ROOT, "Unable to create: %s: %s%n", target, x )
			);
		}

		return CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Files.copy( file, targetBase.resolve( sourceBase.relativize( file ) ) );
		return CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		throw new IllegalStateException(
				String.format( Locale.ROOT, "Problem visiting file `%s`", file ),
				exc
		);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		return CONTINUE;
	}
}
