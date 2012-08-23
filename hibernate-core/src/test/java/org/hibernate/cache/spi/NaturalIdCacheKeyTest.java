/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.spi;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class NaturalIdCacheKeyTest {
    @Test
    public void testSerializationRoundTrip() throws Exception {
        final EntityPersister entityPersister = mock(EntityPersister.class);
        final SessionImplementor sessionImplementor = mock(SessionImplementor.class);
        final SessionFactoryImplementor sessionFactoryImplementor = mock(SessionFactoryImplementor.class);
        final Type mockType = mock(Type.class);
        
        when (entityPersister.getRootEntityName()).thenReturn("EntityName");
        
        when(sessionImplementor.getFactory()).thenReturn(sessionFactoryImplementor);
        
        when(entityPersister.getNaturalIdentifierProperties()).thenReturn(new int[] {0, 1, 2});
        when(entityPersister.getPropertyTypes()).thenReturn(new Type[] {
                mockType,
                mockType,
                mockType
        });
        
        when(mockType.getHashCode(anyObject(), eq(sessionFactoryImplementor))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0].hashCode();
            }
        });
        
        when(mockType.disassemble(anyObject(), eq(sessionImplementor), eq(null))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0];
            }
        });
        
        final NaturalIdCacheKey key = new NaturalIdCacheKey(new Object[] {"a", "b", "c"}, entityPersister, sessionImplementor);
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(key);
        
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final NaturalIdCacheKey keyClone = (NaturalIdCacheKey)ois.readObject();
        
        assertEquals(key, keyClone);
        assertEquals(key.hashCode(), keyClone.hashCode());
        assertEquals(key.toString(), keyClone.toString());
        assertEquals(key.getEntityName(), keyClone.getEntityName());
        assertArrayEquals(key.getNaturalIdValues(), keyClone.getNaturalIdValues());
        assertEquals(key.getTenantId(), keyClone.getTenantId());
        
    }
}
