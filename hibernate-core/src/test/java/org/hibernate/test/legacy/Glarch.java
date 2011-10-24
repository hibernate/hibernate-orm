//$Id: Glarch.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.classic.Lifecycle;

public class Glarch extends Super implements GlarchProxy, Lifecycle, Named, Serializable {

	private int version;
	private GlarchProxy next;
	private short order;
	private List strings;
	private Map stringSets;
	private List fooComponents;
	private GlarchProxy[] proxyArray;
	private Set proxySet;
	//private Currency currency = Currency.getInstance( Locale.getDefault() );
	private transient Map dynaBean;
	private String immutable;
	private int derivedVersion;
	private Object any;
	private int x;
	private Multiplicity multiple;

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public GlarchProxy getNext() {
		return next;
	}
	public void setNext(GlarchProxy next) {
		this.next = next;
	}

	public short getOrder() {
		return order;
	}
	public void setOrder(short order) {
		this.order = order;
	}

	public List getStrings() {
		return strings;
	}

	public void setStrings(List strings) {
		this.strings = strings;
	}

	public Map getStringSets() {
		return stringSets;
	}

	public void setStringSets(Map stringSets) {
		this.stringSets = stringSets;
	}

	public List getFooComponents() {
		return fooComponents;
	}

	public void setFooComponents(List fooComponents) {
		this.fooComponents = fooComponents;
	}

	public GlarchProxy[] getProxyArray() {
		return proxyArray;
	}
	public void setProxyArray(GlarchProxy[] proxyArray) {
		this.proxyArray = proxyArray;
	}
	public Set getProxySet() {
		return proxySet;
	}

	public void setProxySet(Set proxySet) {
		this.proxySet = proxySet;
	}

	public boolean onDelete(Session s) throws CallbackException {
		return NO_VETO;
	}

	public void onLoad(Session s, Serializable id) {
		if ( ! ( ( (String) id ).length()==32 ) ) throw new RuntimeException("id problem");
	}

	public boolean onSave(Session s) throws CallbackException {
		dynaBean = new HashMap();
		dynaBean.put("foo", "foo");
		dynaBean.put("bar", new Integer(66));
		immutable="never changes!";
		return NO_VETO;
	}

	public boolean onUpdate(Session s) throws CallbackException {
		return NO_VETO;
	}

	/*public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}*/

	/**
	 * Returns the dynaBean.
	 * @return DynaBean
	 */
	public Map getDynaBean() {
		return dynaBean;
	}

	/**
	 * Sets the dynaBean.
	 * @param dynaBean The dynaBean to set
	 */
	public void setDynaBean(Map dynaBean) {
		this.dynaBean = dynaBean;
	}

	/**
	 * Returns the immutable.
	 * @return String
	 */
	public String getImmutable() {
		return immutable;
	}

	/**
	 * Sets the immutable.
	 * @param immutable The immutable to set
	 */
	public void setImmutable(String immutable) {
		this.immutable = immutable;
	}

	/**
	 * Returns the derivedVersion.
	 * @return int
	 */
	public int getDerivedVersion() {
		return derivedVersion;
	}

	/**
	 * Sets the derivedVersion.
	 * @param derivedVersion The derivedVersion to set
	 */
	public void setDerivedVersion(int derivedVersion) {
		this.derivedVersion = derivedVersion;
	}

	/**
	 * Returns the any.
	 * @return Object
	 */
	public Object getAny() {
		return any;
	}

	/**
	 * Sets the any.
	 * @param any The any to set
	 */
	public void setAny(Object any) {
		this.any = any;
	}

	/**
	 * @return
	 */
	public Multiplicity getMultiple() {
		return multiple;
	}

	/**
	 * @param multiplicity
	 */
	public void setMultiple(Multiplicity multiplicity) {
		multiple = multiplicity;
	}

	public String getName() {
		return super.getName();
	}

}







