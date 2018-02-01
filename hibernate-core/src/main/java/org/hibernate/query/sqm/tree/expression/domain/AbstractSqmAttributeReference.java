/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmAttributeReference<A extends PersistentAttribute>
		extends AbstractSqmNavigableReference
		implements SqmAttributeReference, SqmFromExporter {

	private final SqmNavigableContainerReference sourceReference;
	private final A attribute;
	private final NavigablePath navigablePath;

	private SqmNavigableJoin join;

	public AbstractSqmAttributeReference(SqmNavigableContainerReference sourceReference, A attribute) {
		if ( sourceReference == null ) {
			throw new IllegalArgumentException( "Source for AttributeBinding cannot be null" );
		}
		if ( attribute == null ) {
			throw new IllegalArgumentException( "Attribute for AttributeBinding cannot be null" );
		}

		this.sourceReference = sourceReference;
		this.attribute = attribute;

		this.navigablePath = sourceReference.getNavigablePath().append( attribute.getAttributeName() );
	}

	@SuppressWarnings("unchecked")
	public AbstractSqmAttributeReference(SqmNavigableJoin join) {
		this(
				join.getNavigableReference().getSourceReference(),
				(A) join.getAttributeReference().getReferencedNavigable()
		);
		injectExportedFromElement( join );
	}

	@Override
	public void injectExportedFromElement(SqmFrom attributeJoin) {
		if ( this.join != null && this.join != attributeJoin ) {
			throw new IllegalArgumentException(
					"Attempting to create multiple SqmFrom references for a single attribute reference : " + getNavigablePath().getFullPath()
			);
		}
		this.join = (SqmNavigableJoin) attributeJoin;
	}

	@Override
	public SqmNavigableContainerReference getSourceReference() {
		// attribute binding must have a source
		return sourceReference;
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return sourceReference;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return attribute.getJavaTypeDescriptor();
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public A getReferencedNavigable() {
		return attribute;
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return join;
	}

	@Override
	public ExpressableType getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String asLoggableText() {
		if ( join == null || join.getIdentificationVariable() == null ) {
			return getClass().getSimpleName() + '(' + sourceReference.asLoggableText() + '.' + attribute.getAttributeName() + ")";
		}
		else {
			return getClass().getSimpleName() + '(' + sourceReference.asLoggableText() + '.' + attribute.getAttributeName() + " : " + join.getIdentificationVariable() + ")";
		}
	}
}
