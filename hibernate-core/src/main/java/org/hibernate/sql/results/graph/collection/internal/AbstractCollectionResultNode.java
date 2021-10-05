/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.collection.CollectionResultGraphNode;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionResultNode implements CollectionResultGraphNode {
	private final NavigablePath navigablePath;
	private final PluralAttributeMapping attributeMapping;

	private final String resultVariable;

	@SuppressWarnings("WeakerAccess")
	protected AbstractCollectionResultNode(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			String resultVariable) {
		this.navigablePath = navigablePath;
		this.attributeMapping = attributeMapping;
		this.resultVariable = resultVariable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}


	@SuppressWarnings("WeakerAccess")
	protected PluralAttributeMapping getAttributeMapping() {
		return attributeMapping;
	}

	@Override
	public JavaTypeDescriptor<?> getResultJavaTypeDescriptor() {
		return attributeMapping.getJavaTypeDescriptor();
	}

	public String getResultVariable() {
		return resultVariable;
	}
}
