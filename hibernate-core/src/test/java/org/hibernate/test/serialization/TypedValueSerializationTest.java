/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.serialization;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.engine.spi.TypedValue;
import org.hibernate.testing.TestForIssue;
import org.hibernate.type.Type;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class TypedValueSerializationTest {
	
	@Test
	@TestForIssue(jiraKey = "HHH-9024")
	public void testTypedValueSerialization() throws Exception {
		final Type mockType = mock(Type.class);
		final String value = "foo";
		final TypedValue typedValue = new TypedValue(mockType, value);
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(typedValue);
        
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final TypedValue typedValueClone = (TypedValue) ois.readObject();
        
        assertEquals(typedValue.hashCode(), typedValueClone.hashCode());
        assertEquals(typedValue.toString(), typedValueClone.toString());
        assertEquals(typedValue.getValue(), typedValueClone.getValue());
	}

}
