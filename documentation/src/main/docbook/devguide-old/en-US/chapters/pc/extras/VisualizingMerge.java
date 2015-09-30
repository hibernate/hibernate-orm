Object detached = ...;
Object managed = entityManager.find( detached.getClass(), detached.getId() );
managed.setXyz( detached.getXyz() );
...
return managed;