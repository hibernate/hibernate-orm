/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.log;

import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.Internal;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Acts as the {@link MessageLogger} and
 * {@link org.jboss.logging.annotations.MessageBundle} for messages related to
 * processing URLs.
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10000001, max = 10001000 )
@SubSystemLogging(
		name = UrlMessageBundle.LOGGER_NAME,
		description = "Logging related to URL handling"
)
@Internal
public interface UrlMessageBundle {
	String LOGGER_NAME = SubSystemLogging.BASE + ".url";

	Logger URL_LOGGER = Logger.getLogger( LOGGER_NAME );
	UrlMessageBundle URL_MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), UrlMessageBundle.class, LOGGER_NAME );

	/**
	 * Logs a warning about a malformed URL, caused by a {@link URISyntaxException}
	 *
	 * @param jarUrl The URL that lead to the {@link URISyntaxException}
	 * @param e The underlying URISyntaxException
	 */
	@LogMessage( level = WARN )
	@Message( value = "Malformed URL: %s", id = 10000001 )
	void logMalformedUrl(URL jarUrl, @Cause URISyntaxException e);

	/**
	 * Logs a warning about not being able to find a file by a specified URL.  This is different
	 * from {@link #logFileDoesNotExist}.
	 *
	 * @param url The URL is supposed to identify the file which we cannot locate
	 * @param e The underlying URISyntaxException
	 */
	@LogMessage( level = WARN )
	@Message( value = "File or directory named by URL [%s] could not be found.  URL will be ignored", id = 10000002 )
	void logUnableToFindFileByUrl(URL url, @Cause Exception e);

	/**
	 * Logs a warning that the File (file/directory) to which the URL resolved
	 * reported that it did not exist.
	 *
	 * @param url The URL that named the file/directory
	 *
	 * @see java.io.File#exists()
	 */
	@LogMessage( level = WARN )
	@Message( value = "File or directory named by URL [%s] did not exist.  URL will be ignored", id = 10000003 )
	void logFileDoesNotExist(URL url);

	/**
	 * Logs a warning indicating that the URL resolved to a File that we were expecting
	 * to be a directory, but {@link java.io.File#isDirectory()} reported it was not.
	 *
	 * @param url The URL that named the file/directory
	 *
	 * @see java.io.File#isDirectory()
	 */
	@LogMessage( level = WARN )
	@Message( value = "Expecting resource named by URL [%s] to be a directory, but it was not.  URL will be ignored", id = 10000004 )
	void logFileIsNotDirectory(URL url);

	/**
	 * Access to the exception message used when a URL references names a file that does not exist.
	 * <p>
	 * TODO : detail when this is a warning {@link #logFileDoesNotExist} versus an exception...
	 *
	 * @param filePart The "file part" that we gleaned from the URL
	 * @param url The given URL
	 *
	 * @return The message
	 */
	@Message( value = "File [%s] referenced by given URL [%s] does not exist", id = 10000005 )
	String fileDoesNotExist(String filePart, URL url);
}
