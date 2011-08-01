package org.hibernate.metamodel.source.annotations.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Strong Liu
 */
public class CompositeHibernateTypeResolver implements HibernateTypeResolver {
    private List<HibernateTypeResolver> resolvers = new ArrayList<HibernateTypeResolver>();
    private final ExplicitHibernateTypeResolver explicitHibernateTypeResolver;

    public CompositeHibernateTypeResolver(ExplicitHibernateTypeResolver explicitHibernateTypeResolver) {
        if ( explicitHibernateTypeResolver == null ) {
            throw new AssertionFailure( "The Given HibernateTypeResolver is null." );
        }
        this.explicitHibernateTypeResolver = explicitHibernateTypeResolver;
    }

    public void addHibernateTypeResolver(HibernateTypeResolver resolver) {
        if ( resolver == null ) {
            throw new AssertionFailure( "The Given HibernateTypeResolver is null." );
        }
        resolvers.add( resolver );
    }

    @Override
    public String getExplicitHibernateTypeName() {
        String type = explicitHibernateTypeResolver.getExplicitHibernateTypeName();
        if ( StringHelper.isEmpty( type ) ) {
            for ( HibernateTypeResolver resolver : resolvers ) {
                type = resolver.getExplicitHibernateTypeName();
                if ( StringHelper.isNotEmpty( type ) ) {
                    break;
                }
            }
        }
        return type;
    }

    @Override
    public Map<String, String> getExplicitHibernateTypeParameters() {
        Map<String, String> parameters = explicitHibernateTypeResolver.getExplicitHibernateTypeParameters();
        if ( CollectionHelper.isEmpty( parameters ) ) {
            for ( HibernateTypeResolver resolver : resolvers ) {
                parameters = resolver.getExplicitHibernateTypeParameters();
                if ( CollectionHelper.isNotEmpty( parameters ) ) {
                    break;
                }
            }
        }
        return parameters;
    }
}
