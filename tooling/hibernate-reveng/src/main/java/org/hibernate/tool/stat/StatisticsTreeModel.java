package org.hibernate.tool.stat;

import java.util.Map;

import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.internal.util.collections.IdentityMap;

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
			return stats.getSecondLevelCacheStatistics( stats.getSecondLevelCacheRegionNames()[index]);
		} else if(parent instanceof SecondLevelCacheStatistics) {
			SecondLevelCacheStatistics slcs = (SecondLevelCacheStatistics) parent;			
			return slcs.getEntries();
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
		} else if(parent instanceof SecondLevelCacheStatistics) {
			/*SecondLevelCacheStatistics stats = (SecondLevelCacheStatistics) parent;
			return stats.getEntries().size();*/
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