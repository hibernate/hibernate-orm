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

import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.CollectionQuerySpace;
import org.hibernate.loader.plan.spi.CompositeQuerySpace;
import org.hibernate.loader.plan.spi.EntityQuerySpace;
import org.hibernate.loader.plan.spi.Join;
import org.hibernate.loader.plan.spi.JoinDefinedByMetadata;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.loader.plan.spi.QuerySpaces;

/**
 * Prints a {@link QuerySpaces} graph as a tree structure.
 * <p/>
 * Intended for use in debugging, logging, etc.
 *
 * @author Steve Ebersole
 */
public class QuerySpaceTreePrinter {
	/**
	 * Singleton access
	 */
	public static final QuerySpaceTreePrinter INSTANCE = new QuerySpaceTreePrinter();

	private QuerySpaceTreePrinter() {
	}

	/**
	 * Returns a String containing the {@link QuerySpaces} graph as a tree structure.
	 *
	 * @param spaces The {@link QuerySpaces} object.
	 * @param aliasResolutionContext The context for resolving table and column aliases
	 *        for the {@link QuerySpace} references in <code>spaces</code>; if null,
	 *        table and column aliases are not included in returned value..
	 * @return the String containing the {@link QuerySpaces} graph as a tree structure.
	 */
	public String asString(QuerySpaces spaces, AliasResolutionContext aliasResolutionContext) {
		return asString( spaces, 0, aliasResolutionContext );
	}

	/**
	 * Returns a String containing the {@link QuerySpaces} graph as a tree structure, starting
	 * at a particular depth.
	 *
	 * The value for depth indicates the number of indentations that will
	 * prefix all lines in the returned String. Root query spaces will be written with depth + 1
	 * and the depth will be further incremented as joined query spaces are traversed.
	 *
	 * An indentation is defined as the number of characters defined by {@link TreePrinterHelper#INDENTATION}.
	 *
	 * @param spaces The {@link QuerySpaces} object.
	 * @param depth The intial number of indentations
	 * @param aliasResolutionContext The context for resolving table and column aliases
	 *        for the {@link QuerySpace} references in <code>spaces</code>; if null,
	 *        table and column aliases are not included in returned value..
	 * @return the String containing the {@link QuerySpaces} graph as a tree structure.
	 */
	public String asString(QuerySpaces spaces, int depth, AliasResolutionContext aliasResolutionContext) {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final PrintStream printStream = new PrintStream( byteArrayOutputStream );
		write( spaces, depth, aliasResolutionContext, printStream );
		printStream.flush();
		return new String( byteArrayOutputStream.toByteArray() );
	}

	/**
	 * Returns a String containing the {@link QuerySpaces} graph as a tree structure, starting
	 * at a particular depth.
	 *
	 * The value for depth indicates the number of indentations that will
	 * prefix all lines in the returned String. Root query spaces will be written with depth + 1
	 * and the depth will be further incremented as joined query spaces are traversed.
	 *
	 * An indentation is defined as the number of characters defined by {@link TreePrinterHelper#INDENTATION}.
	 *
	 * @param spaces The {@link QuerySpaces} object.
	 * @param depth The intial number of indentations
	 * @param aliasResolutionContext The context for resolving table and column aliases
	 *        for the {@link QuerySpace} references in <code>spaces</code>; if null,
	 *        table and column aliases are not included in returned value.
	 * @param printStream The print stream for writing.
	 */
	public void write(
			QuerySpaces spaces,
			int depth,
			AliasResolutionContext aliasResolutionContext,
			PrintStream printStream) {
		write( spaces, depth, aliasResolutionContext, new PrintWriter( printStream ) );
	}

	/**
	 * Returns a String containing the {@link QuerySpaces} graph as a tree structure, starting
	 * at a particular depth.
	 *
	 * The value for depth indicates the number of indentations that will
	 * prefix all lines in the returned String. Root query spaces will be written with depth + 1
	 * and the depth will be further incremented as joined query spaces are traversed.
	 *
	 * An indentation is defined as the number of characters defined by {@link TreePrinterHelper#INDENTATION}.
	 *
	 * @param spaces The {@link QuerySpaces} object.
	 * @param depth The intial number of indentations
	 * @param aliasResolutionContext The context for resolving table and column aliases
	 *        for the {@link QuerySpace} references in <code>spaces</code>; if null,
	 *        table and column aliases are not included in returned value.
	 * @param printWriter The print writer for writing.
	 */
	public void write(
			QuerySpaces spaces,
			int depth,
			AliasResolutionContext aliasResolutionContext,
			PrintWriter printWriter) {
		if ( spaces == null ) {
			printWriter.println( "QuerySpaces is null!" );
			return;
		}

		printWriter.println( TreePrinterHelper.INSTANCE.generateNodePrefix( depth ) + "QuerySpaces" );

		for ( QuerySpace querySpace : spaces.getRootQuerySpaces() ) {
			writeQuerySpace( querySpace, depth + 1, aliasResolutionContext, printWriter );
		}

		printWriter.flush();
	}

	private void writeQuerySpace(
			QuerySpace querySpace,
			int depth,
			AliasResolutionContext aliasResolutionContext,
			PrintWriter printWriter) {
		generateDetailLines( querySpace, depth, aliasResolutionContext, printWriter );
		writeJoins( querySpace.getJoins(), depth + 1, aliasResolutionContext, printWriter );
	}

	final int detailDepthOffset = 1;

	private void generateDetailLines(
			QuerySpace querySpace,
			int depth,
			AliasResolutionContext aliasResolutionContext,
			PrintWriter printWriter) {
		printWriter.println(
				TreePrinterHelper.INSTANCE.generateNodePrefix( depth ) + extractDetails( querySpace )
		);

		if ( aliasResolutionContext == null ) {
			return;
		}

		printWriter.println(
				TreePrinterHelper.INSTANCE.generateNodePrefix( depth + detailDepthOffset )
						+ "SQL table alias mapping - " + aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid(
						querySpace.getUid()
				)
		);

		final EntityReferenceAliases entityAliases = aliasResolutionContext.resolveEntityReferenceAliases( querySpace.getUid() );
		final CollectionReferenceAliases collectionReferenceAliases = aliasResolutionContext.resolveCollectionReferenceAliases( querySpace.getUid() );

		if ( entityAliases != null ) {
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth + detailDepthOffset )
							+ "alias suffix - " + entityAliases.getColumnAliases().getSuffix()
			);
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth + detailDepthOffset )
							+ "suffixed key columns - {"
							+ StringHelper.join( ", ", entityAliases.getColumnAliases().getSuffixedKeyAliases() )
							+ "}"
			);
		}

		if ( collectionReferenceAliases != null ) {
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth + detailDepthOffset )
							+ "alias suffix - " + collectionReferenceAliases.getCollectionColumnAliases().getSuffix()
			);
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth + detailDepthOffset )
							+ "suffixed key columns - {"
							+ StringHelper.join( ", ", collectionReferenceAliases.getCollectionColumnAliases().getSuffixedKeyAliases() )
							+ "}"
			);
			final EntityAliases elementAliases =
					collectionReferenceAliases.getEntityElementAliases() == null ?
							null :
							collectionReferenceAliases.getEntityElementAliases().getColumnAliases();
			if ( elementAliases != null ) {
				printWriter.println(
						TreePrinterHelper.INSTANCE.generateNodePrefix( depth + detailDepthOffset )
								+ "entity-element alias suffix - " + elementAliases.getSuffix()
				);
				printWriter.println(
						TreePrinterHelper.INSTANCE.generateNodePrefix( depth + detailDepthOffset )
								+ elementAliases.getSuffix()
								+ "entity-element suffixed key columns - "
								+ StringHelper.join( ", ", elementAliases.getSuffixedKeyAliases() )
				);
			}
		}
	}

	private void writeJoins(
			Iterable<Join> joins,
			int depth,
			AliasResolutionContext aliasResolutionContext,
			PrintWriter printWriter) {
		for ( Join join : joins ) {
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth ) + extractDetails( join )
			);
			writeQuerySpace( join.getRightHandSide(), depth+1, aliasResolutionContext, printWriter );
		}
	}

	/**
	 * Returns a String containing high-level details about the {@link QuerySpace}, such as:
	 * <ul>
	 *     <li>query space class name</li>
	 *     <li>unique ID</li>
	 *     <li>entity name (for {@link EntityQuerySpace}</li>
	 *     <li>collection role (for {@link CollectionQuerySpace}</li>	 *
	 * </ul>
	 * @param space The query space
	 * @return a String containing details about the {@link QuerySpace}
	 */
	public String extractDetails(QuerySpace space) {
		if ( EntityQuerySpace.class.isInstance( space ) ) {
			final EntityQuerySpace entityQuerySpace = (EntityQuerySpace) space;
			return String.format(
					"%s(uid=%s, entity=%s)",
					entityQuerySpace.getClass().getSimpleName(),
					entityQuerySpace.getUid(),
					entityQuerySpace.getEntityPersister().getEntityName()
			);
		}
		else if ( CompositeQuerySpace.class.isInstance( space ) ) {
			final CompositeQuerySpace compositeQuerySpace = (CompositeQuerySpace) space;
			return String.format(
					"%s(uid=%s)",
					compositeQuerySpace.getClass().getSimpleName(),
					compositeQuerySpace.getUid()
			);
		}
		else if ( CollectionQuerySpace.class.isInstance( space ) ) {
			final CollectionQuerySpace collectionQuerySpace = (CollectionQuerySpace) space;
			return String.format(
					"%s(uid=%s, collection=%s)",
					collectionQuerySpace.getClass().getSimpleName(),
					collectionQuerySpace.getUid(),
					collectionQuerySpace.getCollectionPersister().getRole()
			);
		}
		return space.toString();
	}

	private String extractDetails(Join join) {
		return String.format(
				"JOIN (%s) : %s -> %s",
				determineJoinType( join ),
				join.getLeftHandSide().getUid(),
				join.getRightHandSide().getUid()
		);
	}

	private String determineJoinType(Join join) {
		if ( JoinDefinedByMetadata.class.isInstance( join ) ) {
			return "JoinDefinedByMetadata(" + ( (JoinDefinedByMetadata) join ).getJoinedPropertyName() + ")";
		}

		return join.getClass().getSimpleName();
	}
}
