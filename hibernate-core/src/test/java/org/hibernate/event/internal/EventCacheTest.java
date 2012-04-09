/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.event.internal;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * 2011/10/20 Unit test for code added in EventCache for performance improvement. 
 * @author Wim Ockerman @ CISCO
 * 
 * 
 *
 */
public class EventCacheTest extends TestCase {
    
    public void testEntityToCopyFillFollowedByCopyToEntityMapping() {
        EventCache cache = new EventCache(); 
        Object entity = new Integer(1);
        Object copy = new Integer(2);
        
        cache.put(entity, copy);
        assertTrue(cache.containsKey(entity)); 
        assertFalse(cache.containsKey(copy)); 
        
        assertTrue(cache.invertMap().containsKey(copy));
        assertFalse(cache.invertMap().containsKey(entity));
        
        cache.clear(); 
        assertFalse(cache.containsKey(entity)); 
        assertFalse(cache.invertMap().containsKey(copy));        
    }

    public void testEntityToCopyFillFollowedByCopyToEntityMappingOnRemove() {
        EventCache cache = new EventCache(); 
        Object entity = new Integer(1);
        Object copy = new Integer(2);
        
        cache.put(entity, copy);
        assertTrue(cache.containsKey(entity)); 
        assertFalse(cache.containsKey(copy)); 
        
        assertTrue(cache.invertMap().containsKey(copy));
        assertFalse(cache.invertMap().containsKey(entity));
        
        cache.remove(entity);  
        assertFalse(cache.containsKey(entity)); 
        assertFalse(cache.invertMap().containsKey(copy));        
    }
    
    public void testEntityToCopyFillFollowedByCopyToEntityUsingPutAll() {
        EventCache cache = new EventCache(); 
        Map input = new HashMap();        
        Object entity1 = new Integer(1);
        Object copy1 = new Integer(2);
        input.put(entity1, copy1); 
        Object entity2 = new Integer(3);
        Object copy2 = new Integer(2);
        input.put(entity2, copy2);
        cache.putAll(input);
        
        assertTrue(cache.containsKey(entity1)); 
        assertFalse(cache.containsKey(copy1)); 
        assertTrue(cache.containsKey(entity2)); 
        assertFalse(cache.containsKey(copy2)); 

        assertTrue(cache.invertMap().containsKey(copy1));
        assertFalse(cache.invertMap().containsKey(entity1));

        assertTrue(cache.invertMap().containsKey(copy2));
        assertFalse(cache.invertMap().containsKey(entity2));
    }
    
    public void testEntityToCopyFillFollowedByCopyToEntityMappingUsingPutWithSetOperatedOnArg() {
        EventCache cache = new EventCache(); 
        Object entity = new Integer(1);
        Object copy = new Integer(2);
        
        cache.put(entity, copy, true);
        assertTrue(cache.containsKey(entity)); 
        assertFalse(cache.containsKey(copy)); 
        
        assertTrue(cache.invertMap().containsKey(copy));
        assertFalse(cache.invertMap().containsKey(entity));
        
        cache.clear(); 
        cache.put(entity, copy, false);
        assertTrue(cache.containsKey(entity)); 
        assertFalse(cache.containsKey(copy)); 
    }

    
}
