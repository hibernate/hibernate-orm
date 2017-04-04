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
 * Basic implemementation of {@link XmlDocument}
 *
 * @author Steve Ebersole
 */
public class XmlDocumentImpl implements XmlDocument, Serializable {
	private final Document documentTree;
	private final Origin origin;

	public XmlDocumentImpl(Document documentTree, String originType, String originName) {
		this( documentTree, new OriginImpl( originType, originName ) );
	}

	public XmlDocumentImpl(Document documentTree, Origin origin) {
		this.documentTree = documentTree;
		this.origin = origin;
	}

	@Override
	public Document getDocumentTree() {
		return documentTree;
	}

	@Override
	public Origin getOrigin() {
		return origin;
	}
}
