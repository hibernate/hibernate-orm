package org.hibernate.query.hql.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90003501, max = 90004000 )
@SubSystemLogging(
		name = HqlLogging.LOGGER_NAME,
		description = "Logging related to HQL parsing"
)
@Internal
public interface HqlLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".query.hql";

	HqlLogging QUERY_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), HqlLogging.class, LOGGER_NAME, Locale.ROOT );

	@LogMessage(level = TRACE)
	@Message(id = 90003501, value = "HQL: %s")
	void hql(String query);

	@LogMessage(level = DEBUG)
	@Message(id = 90003502, value = "Raw selection of plural attribute not supported by JPA. Use 'value(%1$s)' or 'key(%1$s)' to indicate what part of the collection to select")
	void rawPluralAttributeSelection(String alias);

	@LogMessage(level = DEBUG)
	@Message(id = 90003503, value = "Questionable sorting by constant value: %s")
	void sortingByConstant(SqmExpression<?> expression);

	@LogMessage(level = WARN)
	@Message(id = 90003504, value = "Misuse of HQL elements() or indices() function, use element() or index() instead")
	void misuseOfElementsOrIndices();

	@LogMessage(level = DEBUG)
	@Message(id = 90003505, value = "Ignoring fetch on entity join: %s(%s)")
	void ignoringFetchOnEntityJoin(String entityName, String alias);
}
