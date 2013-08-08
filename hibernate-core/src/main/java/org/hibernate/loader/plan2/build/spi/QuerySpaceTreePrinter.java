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
package org.hibernate.loader.plan2.build.spi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan2.build.internal.spaces.CollectionQuerySpaceImpl;
import org.hibernate.loader.plan2.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan2.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan2.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan2.spi.CollectionQuerySpace;
import org.hibernate.loader.plan2.spi.CompositeQuerySpace;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.loader.plan2.spi.JoinDefinedByMetadata;
import org.hibernate.loader.plan2.spi.QuerySpace;
import org.hibernate.loader.plan2.spi.QuerySpaces;

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

	public String asString(QuerySpaces spaces, AliasResolutionContext aliasResolutionContext) {
		return asString( spaces, 0, aliasResolutionContext );
	}

	public String asString(QuerySpaces spaces, int depth, AliasResolutionContext aliasResolutionContext) {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final PrintStream printStream = new PrintStream( byteArrayOutputStream );
		write( spaces, depth, aliasResolutionContext, printStream );
		printStream.flush();
		return new String( byteArrayOutputStream.toByteArray() );
	}

	public void write(
			QuerySpaces spaces,
			int depth,
			AliasResolutionContext aliasResolutionContext,
			PrintStream printStream) {
		write( spaces, depth, aliasResolutionContext, new PrintWriter( printStream ) );
	}

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
			final EntityAliases elementAliases = collectionReferenceAliases.getEntityElementColumnAliases();
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
			return "JoinDefinedByMetadata(" + ( (JoinDefinedByMetadata) join ).getJoinedAssociationPropertyName() + ")";
		}

		return join.getClass().getSimpleName();
	}
}
