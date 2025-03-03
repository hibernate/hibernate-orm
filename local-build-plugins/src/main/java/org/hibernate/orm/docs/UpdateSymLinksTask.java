/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.docs;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.orm.ReleaseFamilyIdentifier;

import static org.hibernate.orm.docs.DocumentationPublishing.DOC_SERVER_BASE_DIR;
import static org.hibernate.orm.docs.DocumentationPublishing.SFTP_SERVER;

/**
 * Updates the "current" and "stable" symlinks on the doc server
 *
 * @author Steve Ebersole
 */
public class UpdateSymLinksTask extends DefaultTask {
	public static final String SYMLINKS_TASK_NAME = "updateDocSymLinks";

	private final Property<String> sftpDocServer;
	private final Property<String> serverBaseDir;
	private final Property<ReleaseFamilyIdentifier> buildingFamily;

	public UpdateSymLinksTask() {
		setGroup( "documentation" );
		setDescription( "Updates the 'current' and 'stable' symlinks on the documentation server" );

		buildingFamily = getProject().getObjects().property( ReleaseFamilyIdentifier.class );
		sftpDocServer = getProject().getObjects().property( String.class );
		serverBaseDir = getProject().getObjects().property( String.class );
	}

	public Property<ReleaseFamilyIdentifier> getBuildingFamily() {
		return buildingFamily;
	}

	public Property<String> getSftpDocServer() {
		return sftpDocServer;
	}

	public Property<String> getServerBaseDir() {
		return serverBaseDir;
	}

	@TaskAction
	public void updateSymLinks() throws Exception {
		updateSymLinks( buildingFamily.get().toExternalForm(), sftpDocServer.get(), serverBaseDir.get() );
	}

	private static void updateSymLinks(String releaseName, String sftpServer, String serverBaseDir) throws Exception {
		final File commandFile = createCommandFile( releaseName, serverBaseDir );
		System.out.println( "SFTP command file : " + commandFile.getAbsolutePath() );

		final Process sftpProcess = new ProcessBuilder()
				.command( "sh", "sftp", "-b", commandFile.getAbsolutePath(), sftpServer )
				.redirectInput( ProcessBuilder.Redirect.INHERIT )
				.start();

		ExecutorService service = Executors.newFixedThreadPool( 2 );
		try ( InputStream is = sftpProcess.getInputStream(); InputStream es = sftpProcess.getErrorStream();
			Closeable pool = service::shutdownNow ) {
			service.submit( () -> drain( is, System.out::println ) );
			service.submit( () -> drain( es, System.err::println ) );
			service.shutdown();

			final boolean isFinished = sftpProcess.waitFor( 15, TimeUnit.SECONDS );
			if ( !isFinished ) {
				System.out.println( "Forcibly ending sftp" );
				sftpProcess.destroyForcibly();
			}
		}
	}

	private static void drain(InputStream stream, Consumer<String> consumer) {
		try (InputStreamReader in = new InputStreamReader( stream, StandardCharsets.UTF_8 );
			BufferedReader bufferedReader = new BufferedReader( in ); ) {
			bufferedReader.lines().forEach( consumer );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}


	private static File createCommandFile(String releaseName, String serverBaseDir) throws IOException {
		final File commandFile = File.createTempFile( "hibernate-orm-release-doc-symlink-" + releaseName, "-cmd.txt" );

		try (FileWriter commandFileWriter = new FileWriter( commandFile )) {
			commandFileWriter.write( "cd " + serverBaseDir + "/stable\n" );
			commandFileWriter.write( "-rm orm\n" );
			commandFileWriter.write( String.format( Locale.ROOT, "ln -s ../orm/%s orm\n", releaseName ) );

			commandFileWriter.write( "cd " + serverBaseDir + "/orm\n" );
			commandFileWriter.write( "-rm current\n" );
			commandFileWriter.write( String.format( Locale.ROOT, "ln -s %s current\n", releaseName ) );

			commandFileWriter.flush();
		}

		return commandFile;
	}

	public static void main(String[] args) throws Exception {
		System.out.println( "Starting UpdateSymLinksTask" );
		updateSymLinks( "6.6", SFTP_SERVER, DOC_SERVER_BASE_DIR );
	}
}
