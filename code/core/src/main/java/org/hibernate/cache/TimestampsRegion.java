package org.hibernate.cache;

/**
 * Defines the contract for a cache region which will specifically be used to
 * store entity "update timestamps".
 *
 * @author Steve Ebersole
 */
public interface TimestampsRegion extends GeneralDataRegion {
}
