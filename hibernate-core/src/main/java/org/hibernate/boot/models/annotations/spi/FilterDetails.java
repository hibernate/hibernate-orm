/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;

/**
 * Commonality for filter annotations
 *
 * @see org.hibernate.annotations.Filter
 * @see org.hibernate.annotations.FilterJoinTable
 *
 * @author Steve Ebersole
 */
public interface FilterDetails {
	void apply(JaxbFilterImpl jaxbFilter, XmlDocumentContext xmlDocumentContext);
}
