Search API
==================

Search API's service layer will provide foundation for developers who wants to write client application
for some REST based resource by allowing developer to store the resources in a schema-less storage system
and access this resource as programmable objects as opposed to a REST resource.

Features:
- Transform JSON based resources into objects.
- Perform CRUD operation of the object into a lucene indexing system.
- Schema-less storage provided by lucene.
- Easily to evolve the object structure by changing the way the Algorithm convert the resources.
- Easily to add more resources location by registering more Resolver classes.

Requirements:
- Algorithm to define how to transform REST resource into the correct Java based object.
- Resolver to define where the Search API will find the REST resources.
- Domain object representation of the REST resource.
- Configuration document to tie all the above together.

To include in your code, add maven dependency as follows, replacing {version} with your preferred version:

    <dependency>
        <groupId>com.muzima</groupId>
        <artifactId>search-api</artifactId>
        <version>{version}</version>
    </dependency>

Check the sample in the code to see how to do this. Happy coding!

**Muzima Team**
