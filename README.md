---

## Report: Answers to Questions

### Part 1 — Setup & Discovery

**Q: Explain the default lifecycle of a JAX-RS resource class.**

By default, JAX-RS creates a new instance of each resource class for every incoming
request (per-request lifecycle). This means any instance variables are reset on each
request and cannot be used to store shared state. In this project, all in-memory data
is stored in static fields in the DataStore class, which means all resource instances
share the same data regardless of how many instances are created. If the data were
stored as instance variables instead, each request would start with an empty dataset
and all previously stored data would be lost. Thread safety is also a consideration
since multiple requests can arrive simultaneously and modify the shared static maps
concurrently.

**Q: Why is HATEOAS considered a hallmark of advanced RESTful design?**

HATEOAS (Hypermedia as the Engine of Application State) means that API responses
include links to related resources and available actions, rather than requiring clients
to construct URLs themselves. This benefits client developers because they do not need
to hardcode or memorise endpoint paths — they can navigate the API dynamically by
following the links provided in responses. It also makes the API more resilient to
change, since if endpoint paths are updated, clients following links automatically
adapt without code changes.

---

### Part 2 — Room Management

**Q: What are the implications of returning only IDs versus full room objects?**

Returning only IDs reduces network bandwidth significantly, which is beneficial when
there are thousands of rooms. However, it forces the client to make additional requests
to fetch the details of each room, increasing the number of round trips. Returning full
room objects increases payload size but reduces the number of requests needed, which
is better for clients that need to display full details immediately. The best approach
depends on the use case — a summary list view benefits from IDs only, while a detailed
dashboard benefits from full objects.

**Q: Is the DELETE operation idempotent in your implementation?**

Yes, the DELETE operation is idempotent in this implementation. The first DELETE request
for a room that exists and has no sensors will successfully remove it and return 204 No
Content. Any subsequent DELETE requests for the same room ID will return 404 Not Found,
since the room no longer exists. While the response code differs between the first and
subsequent calls, the end state of the server remains the same — the room does not exist.
This satisfies the definition of idempotency, which requires that the side effects of
multiple identical requests are the same as a single request.

---

### Part 3 — Sensor Operations

**Q: What happens if a client sends data in a format other than JSON?**

The @Consumes(MediaType.APPLICATION_JSON) annotation tells JAX-RS that the endpoint
only accepts JSON. If a client sends a request with a Content-Type of text/plain or
application/xml, JAX-RS will automatically reject the request before it reaches the
resource method and return an HTTP 415 Unsupported Media Type response. This is handled
entirely by the framework without any custom code needed.

**Q: Why is @QueryParam preferred over path-based filtering?**

Using a query parameter such as GET /sensors?type=CO2 is preferred for filtering because
it clearly communicates that the type is an optional filter on a collection rather than
a required identifier. The path /sensors still refers to the full sensors collection,
and the query parameter narrows it down. In contrast, using a path like
/sensors/type/CO2 implies that "type" and "CO2" are resource identifiers, which is
semantically incorrect — they are filter criteria, not resource names. Query parameters
are also more flexible, as multiple filters can be combined easily
(e.g. ?type=CO2&status=ACTIVE), whereas path-based filters become awkward with multiple
parameters.

---

### Part 4 — Sub-Resources

**Q: What are the architectural benefits of the Sub-Resource Locator pattern?**

The Sub-Resource Locator pattern allows complex APIs to be split across multiple
dedicated classes rather than placing every endpoint in one large controller. In this
project, SensorReadingResource handles all reading-related logic, keeping it separate
from SensorResource. This improves maintainability since each class has a single
responsibility, making it easier to find, modify, and test individual parts of the API.
It also improves readability, as each class is smaller and more focused. In large APIs
with many nested resources, this pattern prevents resource classes from growing
unmanageable in size.

---

### Part 5 — Error Handling & Logging

**Q: Why is HTTP 422 more semantically accurate than 404 for a missing linked resource?**

A 404 Not Found response typically means the requested URL/resource does not exist.
When a client POSTs a new sensor with a roomId that does not exist, the URL
/api/v1/sensors is valid and the request was received correctly. The problem is not
that the endpoint is missing, but that the data inside the valid request references a
resource that cannot be found. HTTP 422 Unprocessable Entity more accurately describes
this situation — the request was well-formed and understood, but the semantic content
of the payload is invalid because it references a non-existent room.

**Q: What are the cybersecurity risks of exposing Java stack traces?**

Exposing raw Java stack traces to external API consumers presents several security
risks. Stack traces reveal the internal package structure and class names of the
application, which helps attackers understand the codebase and identify targets.
They can expose the versions of libraries and frameworks being used, allowing attackers
to look up known vulnerabilities for those specific versions. They may also reveal file
paths on the server, database query details, and the logic flow of the application.
The GlobalExceptionMapper in this project prevents this by catching all unexpected
errors and returning only a generic 500 message, keeping internal details hidden.

**Q: Why use JAX-RS filters for logging instead of manual Logger calls?**

Using JAX-RS filters for cross-cutting concerns like logging is far superior to
manually inserting Logger.info() calls in every resource method. Filters are applied
automatically to every request and response without modifying any resource class,
which means there is no risk of forgetting to add logging to a new endpoint. It also
keeps resource classes clean and focused on business logic rather than infrastructure
concerns. This follows the separation of concerns principle and makes the logging
behaviour easy to modify or disable in one place without touching any resource code.
