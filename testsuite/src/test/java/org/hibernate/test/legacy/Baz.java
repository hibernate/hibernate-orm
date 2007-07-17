//$Id: Baz.java 4688 2004-10-26 09:10:50Z oneovthafew $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class Baz implements Named, Serializable, Comparable {
	private SortedSet stringSet;
	private Map stringDateMap;
	private List stringList;
	private int[] intArray;
	private FooProxy[] fooArray;
	private String[] stringArray;
	private String code;
	private List customs;
	private List topComponents;
	private Set fooSet;
	private FooComponent[] components;
	private Date[] timeArray;
	private int count;
	private String name;
	private Collection bag;
	private Set topFoos;
	private Map topGlarchez;
	private Set cascadingBars;
	private Map fooToGlarch;
	private Map fooComponentToFoo;
	private Map glarchToFoo;
	private List fees;
	private Collection fooBag;
	private Set cached;
	private Map cachedMap;
	private Map stringGlarchMap;
	private Map anyToAny;
	private List manyToAny;
	private Collection idFooBag;
	private Collection byteBag;
	private FooProxy foo;
	private List bazez;
	private SortedSet sortablez;
	private NestingComponent collectionComponent;
	private String text;
	private List parts;
	private List moreParts;
	public List subs;
	public Baz superBaz;
	
	Baz() {}
	
	public SortedSet getStringSet() {
		return stringSet;
	}
	public void setStringSet(SortedSet stringSet) {
		this.stringSet = stringSet;
	}
	public Map getStringDateMap() {
		return stringDateMap;
	}
	public void setStringDateMap(Map stringDateMap) {
		this.stringDateMap = stringDateMap;
	}
	public List getStringList() {
		return stringList;
	}
	public void setStringList(List stringList) {
		this.stringList = stringList;
	}
	public int[] getIntArray() {
		return intArray;
	}
	public void setIntArray(int[] intArray) {
		this.intArray = intArray;
	}
	public FooProxy[] getFooArray() {
		return fooArray;
	}
	public void setFooArray(FooProxy[] fooArray) {
		this.fooArray = fooArray;
	}
	public String[] getStringArray() {
		return stringArray;
	}
	public void setStringArray(String[] stringArray) {
		this.stringArray = stringArray;
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	
	public void setDefaults() {
		SortedSet set = new TreeSet();
		set.add("foo"); set.add("bar"); set.add("baz");
		setStringSet(set);
		Map map = new TreeMap();
		map.put( "now", new Date() );
		map.put( "never", null );
		map.put( "big bang", new Date(0) );
		setStringDateMap(map);
		List list = new ArrayList();
		list.addAll(set);
		setStringList(list);
		setIntArray( new int[] { 1,3,3,7 } );
		setFooArray( new Foo[0] );
		setStringArray( (String[]) list.toArray( new String[0] ) );
		customs = new ArrayList();
		customs.add( new String[] { "foo", "bar" } );
		customs.add( new String[] { "A", "B" } );
		customs.add( new String[] { "1", "2" } );
		
		fooSet = new HashSet();
		components = new FooComponent[] {
			new FooComponent("foo", 42, null, null),
			new FooComponent("bar", 88, null, new FooComponent("sub", 69, null, null) )
		};
		timeArray = new Date[] { new Date(), new Date(), null, new Date(0) };
		TreeSet x = new TreeSet();
		x.add("w"); x.add("x"); x.add("y"); x.add("z");
		TreeSet a = new TreeSet();
		a.add("a"); a.add("b"); a.add("d"); a.add("c");
		
		count = 667;
		name="Bazza";
		topComponents = new ArrayList();
		topComponents.add( new FooComponent("foo", 11, new Date[] { new Date(), new Date(123) }, null) );
		topComponents.add( new FooComponent("bar", 22, new Date[] { new Date(7), new Date(456) }, null) );
		topComponents.add( null );
		bag = new ArrayList();
		bag.add("duplicate");
		bag.add("duplicate");
		bag.add("duplicate");
		bag.add("unique");
		cached = new TreeSet();
		CompositeElement ce = new CompositeElement();
		ce.setFoo("foo");
		ce.setBar("bar");
		CompositeElement ce2 = new CompositeElement();
		ce2.setFoo("fooxxx");
		ce2.setBar("barxxx");
		cached.add(ce);
		cached.add(ce2);
		cachedMap = new TreeMap();
		cachedMap.put(this, ce);
		
		text="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		for (int i=0; i<10; i++) text+=text;
		
	}
	
	public List getCustoms() {
		return customs;
	}
	public void setCustoms(List customs) {
		this.customs = customs;
	}
	
	public Set getFooSet() {
		return fooSet;
	}
	public void setFooSet(Set fooSet) {
		this.fooSet = fooSet;
	}
	
	public FooComponent[] getComponents() {
		return components;
	}
	public void setComponents(FooComponent[] components) {
		this.components = components;
	}
	
	public Date[] getTimeArray() {
		return timeArray;
	}
	
	public void setTimeArray(Date[] timeArray) {
		this.timeArray = timeArray;
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List getTopComponents() {
		return topComponents;
	}
	
	public void setTopComponents(List topComponents) {
		this.topComponents = topComponents;
	}
	
	public Collection getBag() {
		return bag;
	}
	
	public void setBag(Collection bag) {
		this.bag = bag;
	}
	
	public Set getTopFoos() {
		return topFoos;
	}
	
	public void setTopFoos(Set topFoos) {
		this.topFoos = topFoos;
	}
	
	
	public Map getTopGlarchez() {
		return topGlarchez;
	}
	
	public void setTopGlarchez(Map topGlarchez) {
		this.topGlarchez = topGlarchez;
	}
	
	public Set getCascadingBars() {
		return cascadingBars;
	}
	
	public void setCascadingBars(Set cascadingBars) {
		this.cascadingBars = cascadingBars;
	}
	
	public Map getFooToGlarch() {
		return fooToGlarch;
	}
	
	public void setFooToGlarch(Map fooToGlarch) {
		this.fooToGlarch = fooToGlarch;
	}
	
	public Map getFooComponentToFoo() {
		return fooComponentToFoo;
	}
	
	public void setFooComponentToFoo(Map fooComponentToFoo) {
		this.fooComponentToFoo = fooComponentToFoo;
	}
	
	public Map getGlarchToFoo() {
		return glarchToFoo;
	}
	
	public void setGlarchToFoo(Map glarchToFoo) {
		this.glarchToFoo = glarchToFoo;
	}
	
	public List getFees() {
		return fees;
	}

	public void setFees(List fees) {
		this.fees = fees;
	}

	public Collection getFooBag() {
		return fooBag;
	}

	public void setFooBag(Collection fooBag) {
		this.fooBag = fooBag;
	}

	/**
	 * Returns the cached.
	 * @return Set
	 */
	public Set getCached() {
		return cached;
	}

	/**
	 * Sets the cached.
	 * @param cached The cached to set
	 */
	public void setCached(Set cached) {
		this.cached = cached;
	}

	/**
	 * Returns the cachedMap.
	 * @return Map
	 */
	public Map getCachedMap() {
		return cachedMap;
	}

	/**
	 * Sets the cachedMap.
	 * @param cachedMap The cachedMap to set
	 */
	public void setCachedMap(Map cachedMap) {
		this.cachedMap = cachedMap;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		return ( (Baz) o ).code.compareTo(code);
	}

	/**
	 * Returns the stringGlarchMap.
	 * @return Map
	 */
	public Map getStringGlarchMap() {
		return stringGlarchMap;
	}

	/**
	 * Sets the stringGlarchMap.
	 * @param stringGlarchMap The stringGlarchMap to set
	 */
	public void setStringGlarchMap(Map stringGlarchMap) {
		this.stringGlarchMap = stringGlarchMap;
	}

	/**
	 * Returns the anyToAny.
	 * @return Map
	 */
	public Map getAnyToAny() {
		return anyToAny;
	}

	/**
	 * Sets the anyToAny.
	 * @param anyToAny The anyToAny to set
	 */
	public void setAnyToAny(Map anyToAny) {
		this.anyToAny = anyToAny;
	}

	public Collection getIdFooBag() {
		return idFooBag;
	}

	public void setIdFooBag(Collection collection) {
		idFooBag = collection;
	}

	public Collection getByteBag() {
		return byteBag;
	}

	public void setByteBag(Collection list) {
		byteBag = list;
	}

	public FooProxy getFoo() {
		return foo;
	}

	public void setFoo(FooProxy foo) {
		this.foo = foo;
	}

	public List getBazez() {
		return bazez;
	}

	public void setBazez(List list) {
		bazez = list;
	}

	public SortedSet getSortablez() {
		return sortablez;
	}

	public void setSortablez(SortedSet set) {
		sortablez = set;
	}


	public NestingComponent getCollectionComponent() {
		return collectionComponent;
	}

	public void setCollectionComponent(NestingComponent collection) {
		collectionComponent = collection;
	}

	/**
	 * @return
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param string
	 */
	public void setText(String string) {
		text = string;
	}

	public List getParts() {
		return parts;
	}

	public void setParts(List list) {
		parts = list;
	}

	public List getManyToAny() {
		return manyToAny;
	}

	public void setManyToAny(List manyToAny) {
		this.manyToAny = manyToAny;
	}

	public List getMoreParts() {
		return moreParts;
	}
	public void setMoreParts(List moreParts) {
		this.moreParts = moreParts;
	}
}







