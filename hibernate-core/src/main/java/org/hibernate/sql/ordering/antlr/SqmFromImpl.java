/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ordering.antlr;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.tree.expression.domain.AbstractSqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmFromImpl implements SqmFrom {
	private final PersistentCollectionDescriptor collectionDescriptor;
	private final SqmFromElementSpace space;
	private final SqmNavigableReference navRef;
	private final String uid;
	private final String alias;

	protected SqmFromImpl(ParsingContext parsingContext, PersistentCollectionDescriptor collectionDescriptor) {
		this.collectionDescriptor = collectionDescriptor;
		this.space = createFromElementSpace();
		this.uid = generateUid();
		this.alias = parsingContext.getImplicitAliasGenerator().buildUniqueImplicitAlias();
		this.navRef = new SqmNavigableReferenceImpl();
	}

	private static SqmFromElementSpace createFromElementSpace() {
		SqmFromClause fromClause = new SqmFromClause();
		return fromClause.makeFromElementSpace();
	}

	private static String generateUid() {
		return "<gen:orderByFragmentParsing>";
	}

	@Override
	public SqmFromElementSpace getContainingSpace() {
		return space;
	}

	@Override
	public String getUniqueIdentifier() {
		return uid;
	}

	@Override
	public SqmNavigableReference getNavigableReference() {
		return navRef;
	}

	@Override
	public String getIdentificationVariable() {
		return alias;
	}

	@Override
	public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		throw new ParsingException(
				"OrderByFragmentParser-generated SqmFrom should not be visited"
		);
	}

	private class SqmNavigableReferenceImpl extends AbstractSqmNavigableReference {

		@Override
		public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
			return null;
		}

		@Override
		public String getUniqueIdentifier() {
			return uid;
		}

		@Override
		public String getIdentificationVariable() {
			return alias;
		}

		@Override
		public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
			return null;
		}

		@Override
		public ExpressableType getExpressionType() {
			return null;
		}

		@Override
		public ExpressableType getInferableType() {
			return null;
		}

		@Override
		public <T> T accept(SemanticQueryWalker<T> walker) {
			throw new ParsingException(
					"OrderByFragmentParser-generated SqmNavigableReference should not be visited"
			);
		}

		@Override
		public String asLoggableText() {
			return String.format(
					Locale.ROOT,
					"{%s -> %s(%s)}",
					OrderByFragmentParser.class.getSimpleName(),
					SqmFromImpl.class.getSimpleName(),
					collectionDescriptor.getNavigableRole().getFullPath()
			);
		}

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			return null;
		}

		@Override
		public SqmNavigableContainerReference getSourceReference() {
			return null;
		}

		@Override
		public Navigable getReferencedNavigable() {
			return null;
		}

		@Override
		public NavigablePath getNavigablePath() {
			return null;
		}

		@Override
		public PersistenceType getPersistenceType() {
			return null;
		}

		@Override
		public Class getJavaType() {
			return null;
		}
	}
}
