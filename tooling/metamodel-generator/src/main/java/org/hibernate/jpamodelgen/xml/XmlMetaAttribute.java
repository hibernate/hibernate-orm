// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.hibernate.jpamodelgen.xml;

import org.hibernate.jpamodelgen.IMetaAttribute;

/**
 * @author Hardy Ferentschik
 */
public abstract class XmlMetaAttribute implements IMetaAttribute {

    private XmlMetaEntity parentEntity;

    private String propertyName;

    private String type;

    XmlMetaAttribute(XmlMetaEntity parent, String propertyName, String type) {
        this.parentEntity = parent;
        this.propertyName = propertyName;
        this.type = type;
    }


	@Override
    public String getDeclarationString() {
        return "public static volatile " + parentEntity.importType(getMetaType()) + "<" + parentEntity.importType(parentEntity.getQualifiedName()) + ", " + parentEntity.importType(getTypeDeclaration()) + "> " + getPropertyName() + ";";
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getTypeDeclaration() {
		return type;
	}

    @Override
    abstract public String getMetaType();
}
