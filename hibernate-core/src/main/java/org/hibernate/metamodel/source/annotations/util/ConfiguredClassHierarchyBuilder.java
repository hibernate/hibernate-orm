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
package org.hibernate.metamodel.source.annotations.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import org.hibernate.metamodel.source.annotations.ConfiguredClassHierarchy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Given a annotation index build a list of class hierarchies.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchyBuilder {

	/**
	 * This methods pre-processes the annotated entities from the index and put them into a structure which can
	 * bound to the Hibernate metamodel.
	 *
	 * @param index The annotation index
	 * @param serviceRegistry The service registry
	 *
	 * @return a set of {@code ConfiguredClassHierarchy}s. One for each configured "leaf" entity.
	 */
	public static Set<ConfiguredClassHierarchy> createEntityHierarchies(Index index, ServiceRegistry serviceRegistry) {
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		Map<ClassInfo, List<ClassInfo>> processedClassInfos = new HashMap<ClassInfo, List<ClassInfo>>();

		for ( ClassInfo info : index.getKnownClasses() ) {
			if ( processedClassInfos.containsKey( info ) ) {
				continue;
			}
			List<ClassInfo> configuredClassList = new ArrayList<ClassInfo>();
			ClassInfo tmpClassInfo = info;
			Class<?> clazz = classLoaderService.classForName( tmpClassInfo.toString() );
			while ( clazz != null && !clazz.equals( Object.class ) ) {
				tmpClassInfo = index.getClassByName( DotName.createSimple( clazz.getName() ) );
				clazz = clazz.getSuperclass();
				if ( tmpClassInfo == null ) {
					continue;
				}

				if ( existsHierarchyWithClassInfoAsLeaf( processedClassInfos, tmpClassInfo ) ) {
					List<ClassInfo> classInfoList = processedClassInfos.get( tmpClassInfo );
					for ( ClassInfo tmpInfo : configuredClassList ) {
						classInfoList.add( tmpInfo );
						processedClassInfos.put( tmpInfo, classInfoList );
					}
					break;
				}
				else {
					configuredClassList.add( 0, tmpClassInfo );
					processedClassInfos.put( tmpClassInfo, configuredClassList );
				}
			}
		}

		Set<ConfiguredClassHierarchy> hierarchies = new HashSet<ConfiguredClassHierarchy>();
		List<List<ClassInfo>> processedList = new ArrayList<List<ClassInfo>>();
		for ( List<ClassInfo> classInfoList : processedClassInfos.values() ) {
			if ( !processedList.contains( classInfoList ) ) {
				hierarchies.add( ConfiguredClassHierarchy.create( classInfoList, serviceRegistry ) );
				processedList.add( classInfoList );
			}
		}

		return hierarchies;
	}

	private static boolean existsHierarchyWithClassInfoAsLeaf(Map<ClassInfo, List<ClassInfo>> processedClassInfos, ClassInfo tmpClassInfo) {
		if ( !processedClassInfos.containsKey( tmpClassInfo ) ) {
			return false;
		}

		List<ClassInfo> classInfoList = processedClassInfos.get( tmpClassInfo );
		return classInfoList.get( classInfoList.size() - 1 ).equals( tmpClassInfo );
	}
}


