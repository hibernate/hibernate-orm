/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.xml;

import java.io.Serializable;

import org.dom4j.Document;

/**
 * Describes a parsed xml document.
 *
 * @author Steve Ebersole
 */
public interface XmlDocument extends Serializable {
	/**
	 * Retrieve the parsed DOM tree.
	 *
	 * @return the DOM tree
	 */
	public Document getDocumentTree();

	/**
	 * Retrieve the document's origin.
	 *
	 * @return The origin
	 */
	public Origin getOrigin();
}
