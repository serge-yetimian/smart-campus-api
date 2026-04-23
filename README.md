# Smart Campus Sensor & Room Management API

## What is this?

This is a REST API built for managing rooms and sensors across a university campus. It's written in Java using JAX-RS (Jersey) with a built-in web server called Grizzly, so there's no need to install Tomcat or anything like that  you just run the JAR file and it starts up.

The API lets you create rooms, assign sensors to them, record sensor readings, and handles errors properly so nothing crashes unexpectedly.

### Endpoints at a glance



 GET  `/api/v1`  Shows API info and links 
 GET  `/api/v1/rooms`  Lists all rooms 
 POST  `/api/v1/rooms`  Creates a new room 
 GET  `/api/v1/rooms/{id}`  Gets one room by ID 
 DELETE  `/api/v1/rooms/{id}`  Deletes a room (only if no sensors are in it) 
 GET | `/api/v1/sensors`  Lists all sensors (can filter by type) 
 POST  `/api/v1/sensors`  Registers a new sensor 
 GET  `/api/v1/sensors/{id}/readings`  Gets all readings for a sensor 
 POST  `/api/v1/sensors/{id}/readings`  Adds a new reading for a sensor 



## What you need before running this

- **Java 11 or newer**  check by running `java -version` in your terminal
- **Maven**  check by running `mvn -version`

If either of those isn't installed, grab them here:
- Java: https://adoptium.net
- Maven: https://maven.apache.org/download.cgi



## How to run it

**Step 1  Clone the repo**
```bash
git clone https://github.com/serge-yetimian/smart-campus-api.git
cd smart-campus-api
```

**Step 2  Build it**
```bash
mvn clean package
```
Wait for it to say `BUILD SUCCESS`. This creates a single JAR file with everything bundled in.

**Step 3  Start the server**
```bash
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```
The server will start at `http://localhost:8080`. That's it  no extra setup needed.

**Step 4  Test it's working**
```bash
curl http://localhost:8080/api/v1
```
You should get back a JSON response with links to the available resources.



## Example curl Commands

Here are some example requests you can run to interact with the API.

### 1. Check the API is running
```bash
curl http://localhost:8080/api/v1
```

### 2. Create a room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "LIB-301", "name": "Library Quiet Study", "capacity": 50}'
```

### 3. Add a sensor to that room
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "CO2-001", "type": "CO2", "status": "ACTIVE", "currentValue": 412.5, "roomId": "LIB-301"}'
```

### 4. Get only CO2 sensors (filtered)
```bash
curl "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 5. Post a reading for a sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 450.0}'
```

### 6. Try to delete a room that still has sensors (expect a 409 error)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 7. Get the reading history for a sensor
```bash
curl http://localhost:8080/api/v1/sensors/CO2-001/readings
```








## Report: Answers to Questions

### Part 1 Setup and Discovery

**Q: Explain the default lifecycle of a JAX-RS resource class.**

In JAX-RS, a new instance of a resource class is created for each incoming request
by default. This means you can't store data in regular instance variables because
they get wiped out after every request. To get around this, I stored all the data
in static fields inside the DataStore class. That way, every resource class reads
and writes to the same shared maps no matter how many instances get created. If I'd
used instance variables instead, every request would start with empty data and
anything previously saved would just disappear. There's also the issue of
concurrency since multiple requests can hit the server at the same time, so using
static maps means I need to be careful about simultaneous reads and writes.

**Q: Why is HATEOAS considered a hallmark of advanced RESTful design?**

HATEOAS basically means your API tells clients where they can go next, rather than
making them guess or rely on documentation. For example, my discovery endpoint at
GET /api/v1/ returns links to /api/v1/rooms and /api/v1/sensors directly in the
response. This is useful because client developers don't need to hardcode URLs.
They just follow the links. It also means if I ever change a path, clients that
navigate via links won't break, whereas clients with hardcoded URLs would. It makes
the API much easier to explore and more resilient to change over time.


### Part 2 Room Management

**Q: What are the implications of returning only IDs versus full room objects?**

There's a tradeoff either way. If you only return IDs, the response is very small
and fast, which is great when you have hundreds of rooms. But then the client has
to make a separate request for each room it wants details on, which adds up quickly.
On the other hand, returning full room objects means one request gives you everything,
but the payload gets much bigger. In my implementation I return full objects because
it's more practical for a campus management system where you typically need the name
and capacity straight away, not just the ID.

**Q: Is the DELETE operation idempotent in your implementation?**

Yes it is. The first time you delete a room that exists and has no sensors, it gets
removed and you get a 204 back. If you send the exact same request again, the room
is already gone so you get a 404. The response code is different but the important
thing is the server's state is the same both times since the room doesn't exist.
That's what idempotency means: sending the same request multiple times has the same
effect as sending it once. So even if a client accidentally sends a duplicate DELETE,
it won't cause any problems.


### Part 3 Sensor Operations

**Q: What happens if a client sends data in a format other than JSON?**

I've annotated the POST endpoint with @Consumes(MediaType.APPLICATION_JSON), which
tells Jersey to only accept JSON. If a client sends the request with a Content-Type
of text/plain or application/xml, Jersey rejects it automatically before my code
even runs and sends back a 415 Unsupported Media Type response. I don't need to
write any extra code to handle this since the framework takes care of it based on
the annotation alone.

**Q: Why is @QueryParam preferred over path-based filtering?**

I think query parameters make much more sense for filtering. With
GET /sensors?type=CO2, it's obvious that you're asking for the sensors collection
and narrowing it down by type. The base resource is still /sensors and the filter
is just optional on top. If I'd done it as /sensors/type/CO2 instead, it looks like
"type" and "CO2" are actual resource identifiers, which is misleading. Query
parameters are also easier to extend. If I later want to filter by both type and
status, I can just add &status=ACTIVE. Doing that with path segments gets messy
very quickly.


### Part 4 Sub-Resources

**Q: What are the architectural benefits of the Sub-Resource Locator pattern?**

The main benefit is that it keeps the code organised and manageable. Instead of
putting every single endpoint into one massive SensorResource class, I created a
separate SensorReadingResource that handles everything related to readings. The
locator method in SensorResource just hands off to it when the path includes
/readings. This means each class has one clear job. SensorResource deals with
sensors and SensorReadingResource deals with readings. If I need to change how
readings work, I go to one file and one file only. In a large real world API with
lots of nested resources, this approach stops things from getting out of hand.


### Part 5 Error Handling and Logging

**Q: Why is HTTP 422 more semantically accurate than 404 for a missing linked resource?**

A 404 tells the client that the URL they requested doesn't exist. But when someone
POSTs to /api/v1/sensors with a roomId that doesn't exist, the URL is perfectly
valid and the problem is with the data inside the request body. The request got
through fine, it was understood, but the roomId it references can't be found. That's
exactly what 422 Unprocessable Entity is for since the syntax is correct but the
content is semantically invalid. Using 404 here would be confusing because the
client might think the /sensors endpoint itself doesn't exist, which isn't the
case at all.

**Q: What are the cybersecurity risks of exposing Java stack traces?**

Showing stack traces to users is a bad idea from a security standpoint. They give
away the internal structure of your application including package names, class names
and line numbers. An attacker can use that to figure out what frameworks and versions
you're using and then look up known vulnerabilities for those exact versions. Stack
traces can also leak file paths, database queries and logic that should stay hidden.
In this project I implemented a GlobalExceptionMapper that catches any unexpected
error and returns a plain 500 message instead. The actual error gets logged server
side where only I can see it and the client just gets a generic response.

**Q: Why use JAX-RS filters for logging instead of manual Logger calls?**

If I added a Logger.info() call to every single resource method, I'd have to
remember to do it for every new endpoint I create and the resource classes would
be cluttered with logging code that has nothing to do with their actual job. Using
a filter means the logging happens automatically for every request and response
without touching any resource class at all. If I want to change the log format or
turn logging off, I change it in one place. It's a much cleaner approach and follows
the idea that things like logging, authentication and error handling should be
separate from your business logic.
