/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.configuration.metadata;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntityXmlMappingData {
    private Document mainXmlMapping;
    private List<Document> additionalXmlMappings;

    public EntityXmlMappingData() {
        mainXmlMapping = DocumentHelper.createDocument();
        additionalXmlMappings = new ArrayList<Document>();
    }

    public Document getMainXmlMapping() {
        return mainXmlMapping;
    }

    public List<Document> getAdditionalXmlMappings() {
        return additionalXmlMappings;
    }

    public Document newAdditionalMapping() {
        Document additionalMapping = DocumentHelper.createDocument();
        additionalXmlMappings.add(additionalMapping);

        return additionalMapping;
    }
}
