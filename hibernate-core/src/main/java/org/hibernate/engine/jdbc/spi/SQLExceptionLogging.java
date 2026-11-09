package org.hibernate.engine.jdbc.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Pos;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.Locale;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Responsible for logging SQL {@linkplain java.sql.SQLException errors}
 * and {@linkplain java.sql.SQLWarning warnings}.
 *
 * @author Gavin King
 * @since 7
 */
@MessageLogger(projectCode = "HHH")
public interface SQLExceptionLogging extends BasicLogger {
	@Nonnull String ERROR_NAME = SubSystemLogging.BASE + ".jdbc.error";
	@Nonnull String WARN_NAME = SubSystemLogging.BASE + ".jdbc.warn";

	@Nonnull SQLExceptionLogging ERROR_LOG = Logger.getMessageLogger( MethodHandles.lookup(), SQLExceptionLogging.class, ERROR_NAME, Locale.ROOT );
	@Nonnull SQLExceptionLogging WARNING_LOG = Logger.getMessageLogger( MethodHandles.lookup(), SQLExceptionLogging.class, WARN_NAME, Locale.ROOT );

	@LogMessage(level = WARN)
	@Message(value = "ErrorCode: %s, SQLState: %s", id = 247)
	void logErrorCodes(@Pos(1) int errorCode, @Pos(2) @Nullable String sqlState);

	@LogMessage(level = DEBUG)
	@Message(value = "Could not log SQL warnings", id = 248)
	void couldNotLogWarnings(@Cause @Nonnull SQLException sqle);

	@LogMessage(level = DEBUG)
	@Message(value = "Could not clear SQL warnings", id = 249)
	void couldNotClearWarnings(@Cause @Nonnull SQLException sqle);
}
