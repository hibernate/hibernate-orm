/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.sql.results.graph.FetchOptions;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractStateArrayContributorMapping
		extends AbstractAttributeMapping
		implements FetchOptions {

	private final AttributeMetadata attributeMetadata;
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;
	private final int stateArrayPosition;

	public AbstractStateArrayContributorMapping(
			String name,
			AttributeMetadata attributeMetadata,
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			int stateArrayPosition,
			int fetchableIndex,
			ManagedMappingType declaringType) {
		super( name, fetchableIndex, declaringType );
		this.attributeMetadata = attributeMetadata;
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
		this.stateArrayPosition = stateArrayPosition;
	}

	public AbstractStateArrayContributorMapping(
			String name,
			AttributeMetadata attributeMetadata,
			FetchOptions mappedFetchOptions,
			int stateArrayPosition,
			int fetchableIndex,
			ManagedMappingType declaringType) {
		this(
				name,
				attributeMetadata,
				mappedFetchOptions.getTiming(),
				mappedFetchOptions.getStyle(),
				stateArrayPosition,
				fetchableIndex,
				declaringType
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected AbstractStateArrayContributorMapping(AbstractStateArrayContributorMapping original) {
		super( original );
		this.attributeMetadata = original.attributeMetadata;
		this.fetchTiming = original.fetchTiming;
		this.fetchStyle = original.fetchStyle;
		this.stateArrayPosition = original.stateArrayPosition;
	}


	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public AttributeMetadata getAttributeMetadata() {
		return attributeMetadata;
	}

	@Override
	public String getFetchableName() {
		return getAttributeName();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return fetchStyle;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}
}
