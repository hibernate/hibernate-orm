/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;

import org.jboss.logging.Logger;

/**
 * Prints a {@link org.hibernate.loader.plan.spi.LoadPlan} graph and its
 * {@link org.hibernate.loader.plan.spi.QuerySpaces} graph as tree structures.
 * <p/>
 * Intended for use in debugging, logging, etc.
 * <p/>
 * Aggregates calls to the {@link QuerySpaceTreePrinter} and {@link ReturnGraphTreePrinter}
 *
 * @author Steve Ebersole
 */
public class LoadPlanTreePrinter {
	private static final Logger log = CoreLogging.logger( LoadPlanTreePrinter.class );

	/**
	 * Singleton access
	 */
	public static final LoadPlanTreePrinter INSTANCE = new LoadPlanTreePrinter();

	private LoadPlanTreePrinter() {
	}

	/**
	 * Logs the specified {@link org.hibernate.loader.plan.spi.LoadPlan} graph and its
	 * {@link org.hibernate.loader.plan.spi.QuerySpaces} graph as tree structures.
	 *
	 * @param loadPlan The load plan.
	 * @param aliasResolutionContext The context for resolving table and column aliases/
	 */
	public void logTree(LoadPlan loadPlan, AliasResolutionContext aliasResolutionContext) {
		if ( ! log.isDebugEnabled() ) {
			return;
		}

		log.debug( toString( loadPlan, aliasResolutionContext ) );
	}

	private String toString(LoadPlan loadPlan, AliasResolutionContext aliasResolutionContext) {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final PrintStream printStream = new PrintStream( byteArrayOutputStream );
		final PrintWriter printWriter = new PrintWriter( printStream );

		logTree( loadPlan, aliasResolutionContext, printWriter );

		printWriter.flush();
		printStream.flush();

		return new String( byteArrayOutputStream.toByteArray() );
	}

	private void logTree(
			LoadPlan loadPlan,
			AliasResolutionContext aliasResolutionContext,
			PrintWriter printWriter) {
		printWriter.println( "LoadPlan(" + extractDetails( loadPlan ) + ")" );
		printWriter.println( TreePrinterHelper.INSTANCE.generateNodePrefix( 1 ) + "Returns" );
		for ( Return rtn : loadPlan.getReturns() ) {
			ReturnGraphTreePrinter.INSTANCE.write( rtn, 2, printWriter );
			printWriter.flush();
		}

		QuerySpaceTreePrinter.INSTANCE.write( loadPlan.getQuerySpaces(), 1, aliasResolutionContext, printWriter );

		printWriter.flush();
	}

	private String extractDetails(LoadPlan loadPlan) {
		switch ( loadPlan.getDisposition() ) {
			case MIXED: {
				return "mixed";
			}
			case ENTITY_LOADER: {
				return "entity=" + ( (EntityReturn) loadPlan.getReturns().get( 0 ) ).getEntityPersister().getEntityName();
			}
			case COLLECTION_INITIALIZER: {
				return "collection=" + ( (CollectionReturn) loadPlan.getReturns().get( 0 ) ).getCollectionPersister().getRole();
			}
			default: {
				return "???";
			}
		}
	}
}
