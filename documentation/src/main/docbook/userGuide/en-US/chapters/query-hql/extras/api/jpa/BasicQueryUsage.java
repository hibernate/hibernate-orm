Query query = ...;
// timeout - in milliseconds
query.setHint( "javax.persistence.query.timeout", 2000 )
// Do not perform (AUTO) implicit flushing
query.setFlushMode( COMMIT );