/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.OldNaturalIdCacheKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        final OldNaturalIdCacheKey key = (OldNaturalIdCacheKey) DefaultCacheKeysFactory.createNaturalIdKey( new Object[] {"a", "b", "c"}, entityPersister, sessionImplementor );

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(key);
        
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final OldNaturalIdCacheKey keyClone = (OldNaturalIdCacheKey) ois.readObject();
        
        assertEquals(key, keyClone);
        assertEquals(key.hashCode(), keyClone.hashCode());
        assertEquals(key.toString(), keyClone.toString());
        assertEquals(key.getEntityName(), keyClone.getEntityName());
        assertArrayEquals(key.getNaturalIdValues(), keyClone.getNaturalIdValues());
        assertEquals(key.getTenantId(), keyClone.getTenantId());
        
    }
}
