/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.caching.mocked;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.NaturalIdCacheKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NaturalIdCacheKeyTest {

    @Test
    public void testSerializationRoundTrip() throws Exception {
        final SessionFactoryImplementor sessionFactoryImplementor = mock( SessionFactoryImplementor.class );
        final SessionImplementor sessionImplementor = mock( SessionImplementor.class );

        final RuntimeMetamodelsImplementor runtimeMetamodels = mock( RuntimeMetamodelsImplementor.class );
        final EntityPersister entityPersister = mock( EntityPersister.class );
        final NaturalIdMapping naturalIdMapping = mock( NaturalIdMapping.class );

        when( sessionImplementor.getFactory() ).thenReturn( sessionFactoryImplementor );
        when( sessionFactoryImplementor.getRuntimeMetamodels()).thenReturn( runtimeMetamodels );
        when( runtimeMetamodels.getEntityMappingType( anyString() ) ).thenReturn( entityPersister );
        when( entityPersister.getRootEntityName() ).thenReturn( "EntityName" );
        when( entityPersister.getNaturalIdMapping() ).thenReturn( naturalIdMapping );
        when( naturalIdMapping.disassemble( any(), eq( sessionImplementor ) ) ).thenAnswer( invocation -> invocation.getArguments()[0] );
        when( naturalIdMapping.calculateHashCode( any() ) ).thenAnswer( invocation -> invocation.getArguments()[0].hashCode() );


        final NaturalIdCacheKey key = (NaturalIdCacheKey) DefaultCacheKeysFactory.staticCreateNaturalIdKey( new Object[] {"a", "b", "c"}, entityPersister, sessionImplementor );

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(key);
        
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final NaturalIdCacheKey keyClone = (NaturalIdCacheKey) ois.readObject();
        
        assertEquals(key, keyClone);
        assertEquals(key.hashCode(), keyClone.hashCode());
        assertEquals(key.toString(), keyClone.toString());
        assertEquals(key.getEntityName(), keyClone.getEntityName());
        assertArrayEquals( (Object[]) key.getNaturalIdValues(), (Object[]) keyClone.getNaturalIdValues() );
        assertEquals(key.getTenantId(), keyClone.getTenantId());
        
    }


    @Test
    public void testSimpleKeyCreation() {

    }
}
