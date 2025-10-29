/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.stat;

import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.tool.reveng.internal.util.AbstractTreeModel;

import java.util.Collections;
import java.util.Map;

public class StatisticsTreeModel extends AbstractTreeModel {

	private final Statistics stats;

	String queries = "Queries";
	String entities = "Entities";
	String collections = "Collections";
	String secondlevelcache = "Cache";
	
	Map<Object, Object> im = IdentityMap.instantiateSequenced( 10 ); 
	
	public StatisticsTreeModel(Statistics stats) {
		this.stats = stats;
	}

	public Object getChild(Object parent, int index) {
		if(parent==stats) {
			switch(index) {
			case 0: return entities;
			case 1: return collections;
			case 2:	return queries;		
			case 3: return secondlevelcache;
			}
		} else if(parent==entities) {
			return stats.getEntityStatistics(stats.getEntityNames()[index]);
		} else if(parent==collections) {
			return stats.getCollectionStatistics(stats.getCollectionRoleNames()[index]);
		} else if(parent==queries) {
			return stats.getQueryStatistics(stats.getQueries()[index]);
		} else if(parent==secondlevelcache) {
			return stats.getCacheRegionStatistics(stats.getSecondLevelCacheRegionNames()[index]);
		} else if (parent instanceof CacheRegionStatistics) {
			return Collections.emptyMap();
		}
		return null;
	}

	public int getChildCount(Object parent) {
		if(parent==stats) {
			return 4;
		} else if(parent==entities) {
			return stats.getEntityNames().length;
		} else if(parent==collections) {
			return stats.getCollectionRoleNames().length;
		} else if(parent==queries) {
			return stats.getQueries().length;
		} else if(parent==secondlevelcache) {
			return stats.getSecondLevelCacheRegionNames().length;
		} else if(parent instanceof CacheRegionStatistics) {
			return 0;
		}
		return 0;
	}

	public int getIndexOfChild(Object parent, Object child) {
		throw new IllegalAccessError();
		//return 0;
	}

	public Object getRoot() {
		return stats;
	}

	public boolean isLeaf(Object node) {
		return false;
	}
	
	public boolean isQueries(Object o) {
		return o==queries; // hack
	}
	
	public boolean isCollections(Object o) {
		return o==collections; // hack
	}
	
	public boolean isEntities(Object o) {
		return o==entities; // hack
	}

	public boolean isCache(Object o) {
		return o==secondlevelcache;
	}
	
	public boolean isContainer(Object o) {
		return isEntities( o ) || isQueries( o ) || isCollections( o ) || isCache( o );
	}
	
}