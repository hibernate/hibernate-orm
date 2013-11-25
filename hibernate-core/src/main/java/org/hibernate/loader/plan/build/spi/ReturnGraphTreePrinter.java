/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.build.spi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.hibernate.loader.plan.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.loader.plan.spi.CollectionFetchableElement;
import org.hibernate.loader.plan.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.ScalarReturn;

/**
 * Prints a {@link Return} graph as a tree structure.
 * <p/>
 * Intended for use in debugging, logging, etc.
 *
 * @author Steve Ebersole
 */
public class ReturnGraphTreePrinter {
	/**
	 * Singleton access
	 */
	public static final ReturnGraphTreePrinter INSTANCE = new ReturnGraphTreePrinter();

	private ReturnGraphTreePrinter() {
	}

	public String asString(Return rootReturn) {
		return asString( rootReturn, 0 );
	}

	public String asString(Return rootReturn, int depth) {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final PrintStream ps = new PrintStream( byteArrayOutputStream );
		write( rootReturn,  depth, ps );
		ps.flush();
		return new String( byteArrayOutputStream.toByteArray() );

	}

	public void write(Return rootReturn, PrintStream printStream) {
		write( rootReturn, new PrintWriter( printStream ) );
	}

	public void write(Return rootReturn, int depth, PrintStream printStream) {
		write( rootReturn, depth, new PrintWriter( printStream ) );
	}

	// todo : see ASTPrinter and try to apply its awesome tree structuring here.
	//		I mean the stuff it does with '|' and '\\-' and '+-' etc as
	//		prefixes for the tree nodes actual text to visually render the tree

	public void write(Return rootReturn, PrintWriter printWriter) {
		write( rootReturn, 0, printWriter );
	}

	public void write(Return rootReturn, int depth, PrintWriter printWriter) {
		if ( rootReturn == null ) {
			printWriter.println( "Return is null!" );
			return;
		}

		printWriter.write( TreePrinterHelper.INSTANCE.generateNodePrefix( depth ) );


		if ( ScalarReturn.class.isInstance( rootReturn ) ) {
			printWriter.println( extractDetails( (ScalarReturn) rootReturn ) );
		}
		else if ( EntityReturn.class.isInstance( rootReturn ) ) {
			final EntityReturn entityReturn = (EntityReturn) rootReturn;
			printWriter.println( extractDetails( entityReturn ) );
			writeEntityReferenceFetches( entityReturn, depth+1, printWriter );
		}
		else if ( CollectionReference.class.isInstance( rootReturn ) ) {
			final CollectionReference collectionReference = (CollectionReference) rootReturn;
			printWriter.println( extractDetails( collectionReference ) );
			writeCollectionReferenceFetches( collectionReference, depth+1, printWriter );
		}

		printWriter.flush();
	}

	private String extractDetails(ScalarReturn rootReturn) {
		return String.format(
				"%s(name=%s, type=%s)",
				rootReturn.getClass().getSimpleName(),
				rootReturn.getName(),
				rootReturn.getType().getName()
		);
	}

	private String extractDetails(EntityReference entityReference) {
		return String.format(
				"%s(entity=%s, querySpaceUid=%s, path=%s)",
				entityReference.getClass().getSimpleName(),
				entityReference.getEntityPersister().getEntityName(),
				entityReference.getQuerySpaceUid(),
				entityReference.getPropertyPath().getFullPath()
		);
	}

	private String extractDetails(CollectionReference collectionReference) {
		// todo : include some form of parameterized type signature?  i.e., List<String>, Set<Person>, etc
		return String.format(
				"%s(collection=%s, querySpaceUid=%s, path=%s)",
				collectionReference.getClass().getSimpleName(),
				collectionReference.getCollectionPersister().getRole(),
				collectionReference.getQuerySpaceUid(),
				collectionReference.getPropertyPath().getFullPath()
		);
	}

	private String extractDetails(CompositeFetch compositeFetch) {
		return String.format(
				"%s(composite=%s, querySpaceUid=%s, path=%s)",
				compositeFetch.getClass().getSimpleName(),
				compositeFetch.getFetchedType().getReturnedClass().getName(),
				compositeFetch.getQuerySpaceUid(),
				compositeFetch.getPropertyPath().getFullPath()
		);
	}

	private void writeEntityReferenceFetches(EntityReference entityReference, int depth, PrintWriter printWriter) {
		if ( BidirectionalEntityReference.class.isInstance( entityReference ) ) {
			return;
		}
		if ( entityReference.getIdentifierDescription().hasFetches() ) {
			printWriter.println( TreePrinterHelper.INSTANCE.generateNodePrefix( depth ) + "(entity id) " );
			writeFetches( ( (FetchSource) entityReference.getIdentifierDescription() ).getFetches(), depth+1, printWriter );
		}

		writeFetches( entityReference.getFetches(), depth, printWriter );
	}

	private void writeFetches(Fetch[] fetches, int depth, PrintWriter printWriter) {
		for ( Fetch fetch : fetches ) {
			writeFetch( fetch, depth, printWriter );
		}
	}

	private void writeFetch(Fetch fetch, int depth, PrintWriter printWriter) {
		printWriter.print( TreePrinterHelper.INSTANCE.generateNodePrefix( depth ) );

		if ( EntityFetch.class.isInstance( fetch ) ) {
			final EntityFetch entityFetch = (EntityFetch) fetch;
			printWriter.println( extractDetails( entityFetch ) );
			writeEntityReferenceFetches( entityFetch, depth+1, printWriter );
		}
		else if ( CompositeFetch.class.isInstance( fetch ) ) {
			final CompositeFetch compositeFetch = (CompositeFetch) fetch;
			printWriter.println( extractDetails( compositeFetch ) );
			writeCompositeFetchFetches( compositeFetch, depth+1, printWriter );
		}
		else if ( CollectionAttributeFetch.class.isInstance( fetch ) ) {
			final CollectionAttributeFetch collectionFetch = (CollectionAttributeFetch) fetch;
			printWriter.println( extractDetails( collectionFetch ) );
			writeCollectionReferenceFetches( collectionFetch, depth+1, printWriter );
		}
	}

	private void writeCompositeFetchFetches(CompositeFetch compositeFetch, int depth, PrintWriter printWriter) {
		writeFetches( compositeFetch.getFetches(), depth, printWriter );
	}

	private void writeCollectionReferenceFetches(
			CollectionReference collectionReference,
			int depth,
			PrintWriter printWriter) {
		final CollectionFetchableIndex indexGraph = collectionReference.getIndexGraph();
		if ( indexGraph != null ) {
			printWriter.print( TreePrinterHelper.INSTANCE.generateNodePrefix( depth ) + "(collection index) " );

			if ( EntityReference.class.isInstance( indexGraph ) ) {
				final EntityReference indexGraphAsEntityReference = (EntityReference) indexGraph;
				printWriter.println( extractDetails( indexGraphAsEntityReference ) );
				writeEntityReferenceFetches( indexGraphAsEntityReference, depth+1, printWriter );
			}
			else if ( CompositeFetch.class.isInstance( indexGraph ) ) {
				final CompositeFetch indexGraphAsCompositeFetch = (CompositeFetch) indexGraph;
				printWriter.println( extractDetails( indexGraphAsCompositeFetch ) );
				writeCompositeFetchFetches( indexGraphAsCompositeFetch, depth+1, printWriter );
			}
		}

		final CollectionFetchableElement elementGraph = collectionReference.getElementGraph();
		if ( elementGraph != null ) {
			printWriter.print( TreePrinterHelper.INSTANCE.generateNodePrefix( depth ) + "(collection element) " );

			if ( EntityReference.class.isInstance( elementGraph ) ) {
				final EntityReference elementGraphAsEntityReference = (EntityReference) elementGraph;
				printWriter.println( extractDetails( elementGraphAsEntityReference ) );
				writeEntityReferenceFetches( elementGraphAsEntityReference, depth+1, printWriter );
			}
			else if ( CompositeFetch.class.isInstance( elementGraph ) ) {
				final CompositeFetch elementGraphAsCompositeFetch = (CompositeFetch) elementGraph;
				printWriter.println( extractDetails( elementGraphAsCompositeFetch ) );
				writeCompositeFetchFetches( elementGraphAsCompositeFetch, depth+1, printWriter );
			}
		}
	}
}
