/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Container.java 6844 2005-05-21 14:22:16Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Container {
	
	public static final class ContainerInnerClass {
		private Simple simple;
		private String name;
		private One one;
		private Many many;
		private int count;
		
		public void setSimple(Simple simple) {
			this.simple = simple;
		}
		
		public Simple getSimple() {
			return simple;
		}
		/**
		 * Returns the name.
		 * @return String
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Sets the name.
		 * @param name The name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name + " = " 
			+ (simple==null ? "nil" : Integer.toString( simple.getCount() ) )
			+ "/" + ( one==null ? "nil" : one.getKey().toString() )
			+ "/" + ( many==null ? "nil" : many.getKey().toString() );
		}
		
		
		/**
		 * Returns the one.
		 * @return One
		 */
		public One getOne() {
			return one;
		}
		
		/**
		 * Sets the one.
		 * @param one The one to set
		 */
		public void setOne(One one) {
			this.one = one;
		}
		
		/**
		 * Returns the many.
		 * @return Many
		 */
		public Many getMany() {
			return many;
		}
		
		/**
		 * Sets the many.
		 * @param many The many to set
		 */
		public void setMany(Many many) {
			this.many = many;
		}
		
		/**
		 * Returns the count.
		 * @return int
		 */
		public int getCount() {
			return count;
		}

		/**
		 * Sets the count.
		 * @param count The count to set
		 */
		public void setCount(int count) {
			this.count = count;
		}
		
		public int hashCode() {
			return count + name.hashCode();
		}
		
		public boolean equals(Object other) {
			ContainerInnerClass cic = (ContainerInnerClass) other;
			return cic.name.equals(name) 
				&& cic.count==count 
				&& cic.one.getKey().equals(one.getKey())
				&& cic.many.getKey().equals(many.getKey())
				&& cic.simple.getCount()==simple.getCount();
		}

	}
	
	private List oneToMany;
	private List manyToMany;
	private List components;
	private Set composites;
	private Collection cascades;
	private long id;
	private Collection bag;
	private Collection lazyBag = new ArrayList();
	private Map ternaryMap;
	private Set ternarySet;
	
	/**
	 * Constructor for Container.
	 */
	public Container() {
		super();
	}
	
	/**
	 * Returns the components.
	 * @return List
	 */
	public List getComponents() {
		return components;
	}
	
	/**
	 * Returns the manyToMany.
	 * @return List
	 */
	public List getManyToMany() {
		return manyToMany;
	}
	
	/**
	 * Returns the oneToMany.
	 * @return List
	 */
	public List getOneToMany() {
		return oneToMany;
	}
	
	/**
	 * Sets the components.
	 * @param components The components to set
	 */
	public void setComponents(List components) {
		this.components = components;
	}
	
	/**
	 * Sets the manyToMany.
	 * @param manyToMany The manyToMany to set
	 */
	public void setManyToMany(List manyToMany) {
		this.manyToMany = manyToMany;
	}
	
	/**
	 * Sets the oneToMany.
	 * @param oneToMany The oneToMany to set
	 */
	public void setOneToMany(List oneToMany) {
		this.oneToMany = oneToMany;
	}
	
	/**
	 * Returns the id.
	 * @return long
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	/**
	 * Gets the composites.
	 * @return Returns a Set
	 */
	public Set getComposites() {
		return composites;
	}
	
	/**
	 * Sets the composites.
	 * @param composites The composites to set
	 */
	public void setComposites(Set composites) {
		this.composites = composites;
	}
	
	/**
	 * Returns the bag.
	 * @return Collection
	 */
	public Collection getBag() {
		return bag;
	}
	
	/**
	 * Sets the bag.
	 * @param bag The bag to set
	 */
	public void setBag(Collection bag) {
		this.bag = bag;
	}
	
	/**
	 * Returns the ternary.
	 * @return Map
	 */
	public Map getTernaryMap() {
		return ternaryMap;
	}
	
	/**
	 * Sets the ternary.
	 * @param ternary The ternary to set
	 */
	public void setTernaryMap(Map ternary) {
		this.ternaryMap = ternary;
	}
	
	public static final class Ternary {
		private String name;
		private Foo foo;
		private Glarch glarch;
		/**
		 * Returns the foo.
		 * @return Foo
		 */
		public Foo getFoo() {
			return foo;
		}
		
		/**
		 * Returns the glarch.
		 * @return Glarch
		 */
		public Glarch getGlarch() {
			return glarch;
		}
		
		/**
		 * Returns the name.
		 * @return String
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Sets the foo.
		 * @param foo The foo to set
		 */
		public void setFoo(Foo foo) {
			this.foo = foo;
		}
		
		/**
		 * Sets the glarch.
		 * @param glarch The glarch to set
		 */
		public void setGlarch(Glarch glarch) {
			this.glarch = glarch;
		}
		
		/**
		 * Sets the name.
		 * @param name The name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		
	}
	
	/**
	 * Returns the ternarySet.
	 * @return Set
	 */
	public Set getTernarySet() {
		return ternarySet;
	}
	
	/**
	 * Sets the ternarySet.
	 * @param ternarySet The ternarySet to set
	 */
	public void setTernarySet(Set ternarySet) {
		this.ternarySet = ternarySet;
	}
	
	/**
	 * Returns the lazyBag.
	 * @return Collection
	 */
	public Collection getLazyBag() {
		return lazyBag;
	}
	
	/**
	 * Sets the lazyBag.
	 * @param lazyBag The lazyBag to set
	 */
	public void setLazyBag(Collection lazyBag) {
		this.lazyBag = lazyBag;
	}
	
	/**
	 * Returns the cascades.
	 * @return Collection
	 */
	public Collection getCascades() {
		return cascades;
	}

	/**
	 * Sets the cascades.
	 * @param cascades The cascades to set
	 */
	public void setCascades(Collection cascades) {
		this.cascades = cascades;
	}

}






