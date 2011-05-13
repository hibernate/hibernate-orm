package org.hibernate.metamodel.source.annotations.xml;

import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 */
public interface MockedNames {
	public static final DotName DEFAULT_ACCESS = DotName.createSimple( "default.access" );
	public static final DotName DEFAULT_DELIMITED_IDENTIFIERS = DotName.createSimple( "default.delimited.identifiers" );
	public static final DotName DEFAULT_ENTITY_LISTENERS = DotName.createSimple( "default.entity.listeners" );
	public static final DotName DEFAULT_POST_LOAD = DotName.createSimple( "default.entity.listener.post.load" );
	public static final DotName DEFAULT_POST_PERSIST = DotName.createSimple( "default.entity.listener.post.persist" );
	public static final DotName DEFAULT_POST_REMOVE = DotName.createSimple( "default.entity.listener.post.remove" );
	public static final DotName DEFAULT_POST_UPDATE = DotName.createSimple( "default.entity.listener.post.update" );
	public static final DotName DEFAULT_PRE_PERSIST = DotName.createSimple( "default.entity.listener.pre.persist" );
	public static final DotName DEFAULT_PRE_REMOVE = DotName.createSimple( "default.entity.listener.pre.remove" );
	public static final DotName DEFAULT_PRE_UPDATE = DotName.createSimple( "default.entity.listener.pre.update" );
}
