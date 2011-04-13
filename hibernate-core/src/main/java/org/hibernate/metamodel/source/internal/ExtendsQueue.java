/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.MappingException;
import org.hibernate.cfg.ExtendsQueueEntry;
import org.hibernate.metamodel.source.hbm.HbmHelper;

import org.jboss.logging.Logger;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ExtendsQueue implements Serializable {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, ExtendsQueue.class.getName());

	private final MetadataImpl metadata;
	private Set<ExtendsQueueEntry> extendsQueue = new HashSet<ExtendsQueueEntry>();

	public ExtendsQueue(MetadataImpl metadata) {
		this.metadata = metadata;
	}

	public void add(ExtendsQueueEntry extendsQueueEntry) {
		extendsQueue.add( extendsQueueEntry );
	}

	public int processExtendsQueue() {
		LOG.debug( "processing extends queue" );
		int added = 0;
//		ExtendsQueueEntry extendsQueueEntry = findPossibleExtends();
//		while ( extendsQueueEntry != null ) {
//			metadata.getMetadataSourceQueue().processHbmXml( extendsQueueEntry.getMetadataXml(), extendsQueueEntry.getEntityNames() );
//			extendsQueueEntry = findPossibleExtends();
//		}
//
//		if ( extendsQueue.size() > 0 ) {
//			Iterator iterator = extendsQueue.iterator();
//			StringBuffer buf = new StringBuffer( "Following super classes referenced in extends not found: " );
//			while ( iterator.hasNext() ) {
//				final ExtendsQueueEntry entry = ( ExtendsQueueEntry ) iterator.next();
//				buf.append( entry.getExplicitName() );
//				if ( entry.getMappingPackage() != null ) {
//					buf.append( "[" ).append( entry.getMappingPackage() ).append( "]" );
//				}
//				if ( iterator.hasNext() ) {
//					buf.append( "," );
//				}
//			}
//			throw new MappingException( buf.toString() );
//		}
//
		return added;
	}

	protected ExtendsQueueEntry findPossibleExtends() {
		Iterator<ExtendsQueueEntry> itr = extendsQueue.iterator();
		while ( itr.hasNext() ) {
			final ExtendsQueueEntry entry = itr.next();
			boolean found = metadata.getEntityBinding( entry.getExplicitName() ) == null
					&& metadata.getEntityBinding( HbmHelper.getClassName( entry.getExplicitName(), entry.getMappingPackage() ) ) != null;
			if ( found ) {
				itr.remove();
				return entry;
			}
		}
		return null;
	}
}
