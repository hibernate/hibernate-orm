/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import java.util.HashMap;

import org.dom4j.Element;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class ManyToManyCollectionElement extends AbstractCollectionElement {

	private final java.util.Map manyToManyFilters = new HashMap();
	private String manyToManyWhere;
	private String manyToManyOrderBy;


	ManyToManyCollectionElement(AbstractPluralAttributeBinding binding) {
		super( binding );
	}

	@Override
	public CollectionElementNature getCollectionElementNature() {
		return CollectionElementNature.MANY_TO_MANY;
	}

	public void fromHbmXml(Element node){
	/*
    <!ELEMENT many-to-many (meta*,(column|formula)*,filter*)>
   	<!ATTLIST many-to-many class CDATA #IMPLIED>
	<!ATTLIST many-to-many node CDATA #IMPLIED>
	<!ATTLIST many-to-many embed-xml (true|false) "true">
	<!ATTLIST many-to-many entity-name CDATA #IMPLIED>
	<!ATTLIST many-to-many column CDATA #IMPLIED>
	<!ATTLIST many-to-many formula CDATA #IMPLIED>
	<!ATTLIST many-to-many not-found (exception|ignore) "exception">
	<!ATTLIST many-to-many outer-join (true|false|auto) #IMPLIED>
	<!ATTLIST many-to-many fetch (join|select) #IMPLIED>
	<!ATTLIST many-to-many lazy (false|proxy) #IMPLIED>
	<!ATTLIST many-to-many foreign-key CDATA #IMPLIED>
	<!ATTLIST many-to-many unique (true|false) "false">
	<!ATTLIST many-to-many where CDATA #IMPLIED>
	<!ATTLIST many-to-many order-by CDATA #IMPLIED>
	<!ATTLIST many-to-many property-ref CDATA #IMPLIED>
    */
	}

	public String getManyToManyWhere() {
		return manyToManyWhere;
	}

	public void setManyToManyWhere(String manyToManyWhere) {
		this.manyToManyWhere = manyToManyWhere;
	}

	public String getManyToManyOrderBy() {
		return manyToManyOrderBy;
	}

	public void setManyToManyOrderBy(String manyToManyOrderBy) {
		this.manyToManyOrderBy = manyToManyOrderBy;
	}
}
