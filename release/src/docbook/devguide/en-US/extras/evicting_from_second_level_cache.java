sessionFactory.evict(Cat.class, catId); //evict a particular Cat

sessionFactory.evict(Cat.class);  //evict all Cats

sessionFactory.evictCollection("Cat.kittens", catId); //evict a particular collection of kittens

sessionFactory.evictCollection("Cat.kittens"); //evict all kitten collections