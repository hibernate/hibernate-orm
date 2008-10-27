/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.revisioninfo;

import org.jboss.envers.RevisionListener;
import org.jboss.envers.tools.reflection.ReflectionTools;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.property.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DefaultRevisionInfoGenerator implements RevisionInfoGenerator {
    private final String revisionInfoEntityName;
    private final RevisionListener listener;
    private final Setter revisionTimestampSetter;
    private final Class<?> revisionInfoClass;

    public DefaultRevisionInfoGenerator(String revisionInfoEntityName, Class<?> revisionInfoClass,
                                       Class<? extends RevisionListener> listenerClass,
                                       String revisionInfoTimestampName) {
        this.revisionInfoEntityName = revisionInfoEntityName;
        this.revisionInfoClass = revisionInfoClass;

        revisionTimestampSetter = ReflectionTools.getSetter(revisionInfoClass, revisionInfoTimestampName);

        if (!listenerClass.equals(RevisionListener.class)) {
            // This is not the default value.
            try {
                listener = listenerClass.newInstance();
            } catch (InstantiationException e) {
                throw new MappingException(e);
            } catch (IllegalAccessException e) {
                throw new MappingException(e);
            }
        } else {
            // Default listener - none
            listener = null;
        }
    }

    private Object newRevision() {
        Object revisionInfo;
        try {
            revisionInfo = revisionInfoClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        revisionTimestampSetter.set(revisionInfo, System.currentTimeMillis(), null);

        if (listener != null) {
            listener.newRevision(revisionInfo);
        }

        return revisionInfo;
    }

    public Object generate(Session session) {
        Object revisionData = newRevision();
        session.save(revisionInfoEntityName, revisionData);
        return revisionData;
    }
}
