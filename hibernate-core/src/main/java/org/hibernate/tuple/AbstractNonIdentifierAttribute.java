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
package org.hibernate.tuple;

import org.hibernate.FetchMode;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNonIdentifierAttribute extends AbstractAttribute implements NonIdentifierAttribute {
	private final AttributeSource source;
	private final SessionFactoryImplementor sessionFactory;

	private final int attributeNumber;

	private final BaselineAttributeInformation attributeInformation;

	protected AbstractNonIdentifierAttribute(
			AttributeSource source,
			SessionFactoryImplementor sessionFactory,
			int attributeNumber,
			String attributeName,
			Type attributeType,
			BaselineAttributeInformation attributeInformation) {
		super( attributeName, attributeType );
		this.source = source;
		this.sessionFactory = sessionFactory;
		this.attributeNumber = attributeNumber;
		this.attributeInformation = attributeInformation;
	}

	@Override
	public AttributeSource getSource() {
		return source();
	}

	protected AttributeSource source() {
		return source;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected int attributeNumber() {
		return attributeNumber;
	}

	@Override
	public boolean isLazy() {
		return attributeInformation.isLazy();
	}

	@Override
	public boolean isInsertable() {
		return attributeInformation.isInsertable();
	}

	@Override
	public boolean isUpdateable() {
		return attributeInformation.isUpdateable();
	}

	@Override
	public ValueGeneration getValueGenerationStrategy() {
		return attributeInformation.getValueGenerationStrategy();
	}

	@Override
	public boolean isNullable() {
		return attributeInformation.isNullable();
	}

	@Override
	public boolean isDirtyCheckable() {
		return attributeInformation.isDirtyCheckable();
	}

	@Override
	public boolean isDirtyCheckable(boolean hasUninitializedProperties) {
		return isDirtyCheckable() && ( !hasUninitializedProperties || !isLazy() );
	}

	@Override
	public boolean isVersionable() {
		return attributeInformation.isVersionable();
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		return attributeInformation.getCascadeStyle();
	}

	@Override
	public FetchMode getFetchMode() {
		return attributeInformation.getFetchMode();
	}

	protected String loggableMetadata() {
		return "non-identifier";
	}

	@Override
	public String toString() {
		return "Attribute(name=" + getName() + ", type=" + getType().getName() + " [" + loggableMetadata() + "])";
	}
}
