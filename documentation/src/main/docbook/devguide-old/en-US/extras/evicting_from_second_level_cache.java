sessionFactory.getCache().containsEntity(Cat.class, catId); // is this particular Cat currently in the cache

sessionFactory.getCache().evictEntity(Cat.class, catId); // evict a particular Cat

sessionFactory.getCache().evictEntityRegion(Cat.class);  // evict all Cats

sessionFactory.getCache().evictEntityRegions();  // evict all entity data

sessionFactory.getCache().containsCollection("Cat.kittens", catId); // is this particular collection currently in the cache

sessionFactory.getCache().evictCollection("Cat.kittens", catId); // evict a particular collection of kittens

sessionFactory.getCache().evictCollectionRegion("Cat.kittens"); // evict all kitten collections

sessionFactory.getCache().evictCollectionRegions(); // evict all collection data