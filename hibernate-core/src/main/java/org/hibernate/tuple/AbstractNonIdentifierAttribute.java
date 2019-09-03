/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		return isDirtyCheckable();
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
