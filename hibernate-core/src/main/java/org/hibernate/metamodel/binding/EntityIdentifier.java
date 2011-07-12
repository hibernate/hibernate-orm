/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Binds the entity identifier.
 *
 * @author Steve Ebersole
 */
public class EntityIdentifier {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EntityIdentifier.class.getName()
	);

	private final EntityBinding entityBinding;
	private SimpleAttributeBinding attributeBinding;
	private IdentifierGenerator identifierGenerator;
	private IdGenerator idGenerator;
	private boolean isIdentifierMapper = false;
	// todo : mappers, etc

	/**
	 * Create an identifier
	 *
	 * @param entityBinding the entity binding for which this instance is the id
	 */
	public EntityIdentifier(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public SimpleAttributeBinding getValueBinding() {
		return attributeBinding;
	}

	public void setValueBinding(SimpleAttributeBinding attributeBinding) {
		if ( this.attributeBinding != null ) {
			// todo : error?  or just log?  For now throw exception and see what happens. Easier to see whether this
			// method gets called multiple times
			LOG.entityIdentifierValueBindingExists( entityBinding.getEntity().getName() );
		}
		this.attributeBinding = attributeBinding;
	}

	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	public boolean isEmbedded() {
		return attributeBinding.getValuesSpan()>1;
	}

	public boolean isIdentifierMapper() {
		return isIdentifierMapper;
	}

	public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory) {
		if ( identifierGenerator == null ) {
			Properties props = new Properties();
			if ( idGenerator != null ) {
				props.putAll( idGenerator.getParameters() );
			}
			identifierGenerator = factory.createIdentifierGenerator(
					idGenerator.getStrategy(),
					getValueBinding().getHibernateTypeDescriptor().getResolvedTypeMapping(),
					props
			);
		}
		return identifierGenerator;
	}

	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}
}
