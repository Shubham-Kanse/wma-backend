---
name: spring-boot-clean-code
description: Write, refactor, and review Java Spring Boot application code using a comprehensive clean-code system for naming, methods, comments, formatting, objects and data, exceptions, integration boundaries, and tests. Use for Java or Spring Boot implementation, bug fixes, refactors, and code reviews; do not use for non-Java work.
---

# Spring Boot Clean Code

Write Java and Spring Boot code that reveals intent, keeps behavior honest, owns its boundaries, and remains safe to change. Apply every relevant rule below; do not collapse this guide into generic SOLID advice. Preserve the repository's established architecture and formatting unless the task explicitly asks to change them.

## Working Method

1. Read the request, nearby code, callers, tests, configuration, and repository instructions before editing.
2. State the behavior in domain language. Identify the highest-level operation and the stable public contract.
3. Add or update the smallest behavior-focused test that would fail without the change. For a refactor, first ensure tests protect the behavior being preserved.
4. Implement the simplest complete change at one level of abstraction. Reuse an existing seam before inventing a new one.
5. Refactor names, method size, duplication, arguments, side effects, comments, exception translation, and boundaries while tests stay green. Clean code is normally rewritten, not produced perfectly on the first pass.
6. Run the narrow test first, then the relevant module suite, formatter, static analysis, and build checks available in the project.

Do not create an interface, wrapper, factory, strategy hierarchy, DTO, or layer merely because this guide mentions one. Add it when the code has the matching pressure: a third-party boundary, repeated type dispatch, related arguments that form a concept, an unavailable dependency, or a public contract that must remain stable.

## Names Must Carry Meaning

### Reveal intent

A name should answer why the thing exists, what it represents or does, and how it is used. A reader should not need to trace assignments or call sites to translate it.

```java
// Avoid
BigDecimal d;
List<User> x = users.stream().filter(User::isActive).toList();

// Prefer
BigDecimal discountAmount;
List<User> activeUsers = users.stream().filter(User::isActive).toList();
```

Write a name once for the compiler; choose it for the hundreds of human reads that follow.

### Do not misinform

Never promise a shape or behavior the code does not provide.

- Do not call a `Map` a `list`, an unordered collection a `queue`, or a destructive method `check...`.
- Avoid names that differ only by tiny visual changes.
- Avoid visually ambiguous characters such as lowercase `l`, uppercase `O`, `1`, and `0` where confusion is plausible.
- Rename stale concepts immediately when behavior or representation changes.

```java
// Avoid: the name lies about the type
Map<UserId, User> userList;

// Prefer
Map<UserId, User> usersById;
```

### Make meaningful distinctions

Different names must describe real differences in role or behavior. Numeric suffixes and vague noise words do not create meaning.

- Avoid `data1`, `data2`, `CustomerInfo`, `CustomerData`, `UserManager`, and `OrderHandler` when the names do not identify distinct responsibilities.
- Prefer role names such as `PayrollCalculator`, `InventoryReservation`, `OrderPolicy`, `PaymentGateway`, and `ShipmentScheduler`.
- Spring suffixes such as `Controller`, `Repository`, and `Configuration` are useful only when the prefix names a specific domain responsibility. Do not let every non-framework class become a `Manager`, `Helper`, `Util`, `Processor`, or `Handler`.

### Use pronounceable names

Programming is social. Names must be speakable in code review, incident response, and onboarding. Prefer natural domain language over compressed syllables and private abbreviations.

```java
// Avoid
LocalDateTime genymdhms;

// Prefer
LocalDateTime generationTimestamp;
```

### Use searchable names

The wider the scope, the more specific and searchable the name must be.

- Reserve single-letter variables for conventional, tiny local scopes where the meaning is immediate, such as `i` in a short index loop.
- Name constants instead of scattering raw numbers or strings.
- Prefer a domain term that can be found reliably across the repository.

```java
private static final int WORKDAYS_PER_WEEK = 5;
private static final Duration PAYMENT_TIMEOUT = Duration.ofSeconds(4);
```

### Avoid type encodings and mental mapping

Modern Java IDEs and the compiler already expose types. Do not encode them into names with Hungarian notation, prefixes, or suffixes such as `strName`, `iCount`, or `userMapField`. Let names describe meaning.

Do not force readers to remember that `tx` means transaction, `p` means price, `t` means tax, and `q` means quantity. Replace private shorthand with the domain vocabulary.

### Name methods one abstraction above their implementation

A technically accurate name can still be poor if it merely narrates the code inside. Name the purpose achieved, not the comparison, loop, or data manipulation used to achieve it.

```java
// Avoid: restates the implementation
boolean isCreatedAtBeforeCutoff(Order order);

// Prefer: explains why the comparison exists
boolean isEligibleForArchival(Order order);
```

This keeps callers readable and allows the implementation to evolve without making the name stale.

## Methods Must Tell a Short, Honest Story

### Keep methods small, then look for smaller

Long methods bury the algorithm under detail. Extract low-level work behind names that reveal the story.

Prefer branch and loop bodies that delegate through one intention-revealing call when that exposes a distinct concept:

```java
void upload(FileUpload upload) {
    if (upload.exceedsSinglePartLimit()) {
        uploadInParts(upload);
        return;
    }
    uploadDirectly(upload);
}
```

The point is not an arbitrary line limit. The point is that a reader sees the decision without carrying multipart mechanics at the same time.

### Do one thing

Use both tests from the transcript:

1. If a method can be divided into separately named sections, it does more than one thing.
2. If a block can be extracted under a name that is more meaningful than a restatement of its implementation, it contains another responsibility.

Stop extracting when the only possible helper name merely repeats the original method's purpose.

### Stay at one level of abstraction

Read the code as a top-down narrative. A high-level Spring application service may say:

```java
public OrderReceipt placeOrder(PlaceOrderCommand command) {
    InventoryReservation reservation = reserveInventory(command.items());
    PricedOrder pricedOrder = priceOrder(command, reservation);
    PaymentReceipt payment = chargeCustomer(command.payment(), pricedOrder.total());
    return confirmOrder(command, reservation, pricedOrder, payment);
}
```

It should not also contain raw SQL, HTTP header parsing, Stripe SDK syntax, JSON traversal, or low-level retry loops. Each helper should descend one level. Let controllers speak HTTP, application services speak use cases, domain objects speak business rules, repositories speak persistence, and adapters speak external protocols.

### Keep type dispatch in one place

Repeated `switch` or `if/else` dispatch on a type causes every operation to know every type. When the same discriminator appears across pay calculation, benefits, scheduling, reporting, or similar operations, introduce polymorphism.

- Define a domain interface or sealed hierarchy.
- Put behavior on each type or use Spring strategy beans.
- Keep object creation or discriminator mapping in one factory/registry.
- After creation, call behavior without repeating the switch.

A single small switch over a closed, stable enum is not a reason to build a hierarchy. The trigger is repeated dispatch or expected growth in types.

### Keep argument lists light

Zero arguments are easiest to call, one is clear, two can be acceptable, and three are a strong signal to group related values. More arguments increase ordering mistakes and test combinations.

Use a Java `record`, value object, command, or criteria object when the values form one concept:

```java
public record CreateInvoiceCommand(
        CustomerId customerId,
        BillingPeriod billingPeriod,
        Currency currency) {}
```

Do not hide unrelated dependencies inside a parameter object. Group values because they belong together, not merely to reduce a count.

### Make dyads and triads obvious

Some pairs are natural, such as `Point(x, y)`. For other two-argument calls, make one value the owner when that reveals the relationship. Avoid calls whose order must be memorized.

Triads require special caution. Keep them only when they have a rigid, conventional order, and encode that order in the method or type name where useful, such as `Insets.of(top, right, bottom, left)`.

Method names should read as an action applied to a noun. `assertExpectedEqualsActual(...)` is clearer than an ambiguous two-value comparison if project conventions permit it.

### Do not pass control flags

A boolean is fine when it is the data being set, such as `setVisible(true)`. It is a flag when it selects which operation a method performs.

```java
// Avoid
publish(articleId, true);

// Prefer
publishImmediately(articleId);
schedulePublication(articleId, publishAt);
```

Flags make one method contain multiple jobs. Split the jobs and name each one.

### Avoid output arguments

Data should enter through arguments and leave through return values. Do not mutate a supplied holder merely to return an answer. Return a value, result record, or changed aggregate explicitly.

A single argument should usually represent one of three shapes:

- A question: input goes in, an answer comes back.
- A transformation: input goes in, a new value comes back.
- An event: something happened, and the method reacts.

### Keep side effects honest

A method named `passwordMatches` must not also start a session. A method named `validate` must not save, publish, charge, send, or delete unless the name makes that effect explicit.

Split hidden effects into separate methods or choose a name that truthfully describes the whole operation. Prefer separation when the combined name becomes awkward; an awkward name often exposes mixed responsibilities.

### Separate commands from queries

A method should either change state or answer a question, not both.

```java
if (!attributes.containsKey(name)) {
    attributes.put(name, value);
}
```

Do not use a method such as `boolean setAttribute(...)` when callers cannot tell whether the boolean means “the write succeeded” or “the attribute already existed.” Expose a query and a command with separate, honest contracts.

### Remove duplicated knowledge

DRY means every piece of knowledge has one unambiguous, authoritative representation. Centralize shared timeout rules, validation policies, calculations, mappings, and protocol handling so one change is made in one place.

Do not merge code merely because two blocks currently look alike. Deduplicate the rule or knowledge that must change together, not coincidental syntax that may evolve independently.

### Rewrite under protection

Do not expect first-draft code to satisfy every rule. Get the idea working under tests, then split methods, improve names, remove duplication, reduce arguments, and align abstraction levels. Messy first thinking is acceptable; shipping it without the rewrite is not.

## Comments and Documentation

### Treat every comment as a maintenance liability

Comments can lie because code changes while prose remains. First ask whether naming, extraction, a value object, or a constant can make the code speak for itself.

```java
private static final int GRACE_DAYS_FOR_WESTERN_TIME_ZONES = 1;
```

Prefer that to a raw `+ 1` followed by a vague comment.

### Write comments only for information code cannot express

Useful comments include:

- The business or regulatory reason behind a non-obvious constraint.
- A warning that prevents an attractive but unsafe “optimization,” especially around concurrency or performance.
- Plain-language translation of a regex, mathematical comparison, protocol quirk, or non-intuitive formula.
- Amplification explaining why a small-looking line is essential, such as a time-zone grace day.
- A precise `TODO` blocked by an external dependency, with the constraint or reason it cannot be completed now and an issue reference when the project uses one.
- Public API Javadoc that lets external consumers use a library without reading its implementation.
- Required copyright and license headers, preferably referring to a standard license instead of restating a legal contract.

Accuracy is mandatory. When code changes, update or remove the nearby comment in the same change.

### Delete comments that add no knowledge

Remove:

- Redundant comments that restate the next line.
- Noise comments written only because a rule says every method needs one.
- Misleading summaries that describe only one branch of the actual behavior.
- Vague prose such as “handle edge case,” “not sure,” or unexplained initials and names.
- Comments about defaults, policies, or code located in another file; put the explanation next to the source of truth.
- Long historical narratives, dead bug stories, and irrelevant RFC archaeology.
- HTML-heavy comments that are unreadable in the editor; prefer plain text and ordinary Javadoc markup only where rendered API documentation needs it.

Noise trains readers to skip comments, causing the rare important warning to be missed.

### Delete commented-out code and fake history

Version control already preserves deleted code, authorship, and change history. Delete commented-out blocks, author attributions that become stale, and journal comments listing past edits. Source files should contain what runs today.

### Do not use comments to apologize for structure

Closing-brace comments usually reveal an oversized or deeply nested method. Position-marker banners often reveal a class with unrelated responsibilities. Fix the method or class instead.

A restrained position marker can be acceptable when framework-required boilerplate naturally forms stable groups and the marker clearly improves navigation. Do not let markers become camouflage for a class that should be split.

## Formatting and Source Order

### Make files read like newspapers

Put the big picture before supporting detail. Within the repository's Java member-order convention:

- Keep fields in one consistent designated location, normally near the top of the class.
- Place constructors together.
- Put the stable public operations before private implementation details.
- Place private helpers near their callers and, where practical, in the order the public method invokes them.
- Group methods with conceptual affinity and consistent naming.

The reader should descend from headline to detail without jumping randomly around the file.

### Use vertical spacing to show concepts

Blank lines separate concepts; density shows which statements belong together. Avoid both walls of text and a blank line after every statement. Group extraction, business decisions, and result construction into readable units.

Use consistent indentation so scope is visible before the code is fully read. Let the project's formatter settle brace style, wrapping, and imports; do not create formatting debates inside feature work.

### Declare things close to use

Declare local variables at the narrowest useful scope immediately before use. Every early declaration is a fact the reader must carry in memory.

Class fields are different: keep them together according to the team's standard because they describe shared object state. Do not scatter fields beside whichever method happens to use one of them.

## Objects, Data, and Encapsulation

### Do not turn private fields into public structure with boilerplate accessors

Private fields plus getters and setters for everything still expose representation. Abstraction means exposing what callers can do, not mirroring how values are stored.

```java
public interface FuelGauge {
    Percentage fuelRemaining();
}
```

This is more abstract than `getGallons()` and `setGallons(...)`. It leaves fuel type, units, and storage representation private.

Expose atomic operations when invariants require values to change together. Avoid public setters that allow half-valid states. Be especially cautious with Lombok `@Data` on JPA entities and domain objects; it can expose every field, create unsafe equality, and erase intentional boundaries.

### Choose objects or data structures deliberately

Objects hide data and expose behavior. Data structures expose data and contain little or no domain behavior. Neither is always superior.

- Choose data structures plus procedures when new operations are expected more often than new variants. Adding an operation changes the procedure set but leaves existing data types stable.
- Choose polymorphic objects when new variants are expected more often than new operations. Adding a variant adds a class while existing callers remain stable.

This is the expression-problem trade-off: procedural designs make operations easier to add; object-oriented designs make types easier to add. Design for the likely axis of change instead of declaring everything an object.

### Avoid hybrids

A class that exposes all its data and also owns substantial business behavior gets the disadvantages of both models. External callers couple to representation while behavior remains trapped inside.

For Spring/JPA code, choose deliberately:

- A rich domain entity hides mutable state and exposes invariant-preserving behavior; persistence concerns adapt around it.
- A persistence data model carries mapped data and navigation while business rules live in domain objects or application/domain services.

Do not casually mix an anemic public data bag, unrestricted setters, persistence navigation, and scattered business rules in the same entity.

### Follow the Law of Demeter for objects

Talk to friends, not strangers. A method may call methods on:

- Its own object.
- Objects it creates.
- Objects passed as arguments.
- Objects held directly as fields.

Avoid reaching through returned objects with chains such as `context.getOptions().getScratchDirectory().getAbsolutePath()`. Ask the responsible object for the outcome you need, such as `context.createScratchFile(...)`.

Demeter applies to behavior-hiding objects, not plain data structures intentionally exposing data. Do not mechanically wrap DTO field access. Also do not “fix” a chain by creating a method whose name merely encodes the entire chain; move the meaningful operation to the owner.

## Errors, Exceptions, and Absence

### Prefer exceptions to error codes

Returned error codes force nested checks, immediate handling at every call site, and shared enums that couple unrelated files. Let the happy-path algorithm read as a sequence of steps and use exceptions for failures.

Keep normal processing and error processing separate. In Spring Boot:

- Throw application- or domain-owned exceptions from use cases and domain rules.
- Translate them to HTTP responses centrally with `@RestControllerAdvice`.
- Translate persistence, SDK, or HTTP-client exceptions at their adapter boundary.
- Do not wrap every line in a `try/catch`; catch only where the code can recover, add useful context, clean up, or translate the abstraction.

### Define the failure contract before risky logic

When a method is expected to translate failures, design the `try/catch/finally` contract first and write the failure test before filling in the happy-path logic. Decide which exception callers should see, what cleanup always runs, and which details remain internal.

The `try/catch` is part of the method's contract. Keep new logic inside that boundary so callers do not inherit accidental library exceptions.

Use Java's try-with-resources instead of manual `finally` cleanup for `AutoCloseable` resources.

### Own exception types at external boundaries

Library exception hierarchies describe the library, not the application. If several SDK exceptions mean the same thing to the application, a wrapper should translate them into one application-owned exception. If the application needs distinct recovery paths, define that split in domain language.

```java
public interface ObjectStore {
    StoredObject put(ObjectKey key, byte[] content);
}

@Component
final class S3ObjectStore implements ObjectStore {
    // Translate S3 SDK failures to StorageFailure here.
}
```

The rest of the code depends on `ObjectStore` and `StorageFailure`, not on the current vendor.

### Do not use exceptions as ordinary branches

Exceptions are for operations that failed, not for expected alternatives. If “no regional rule” means “use the standard rule,” return a `StandardTaxRule` instead of throwing and catching a missing-rule exception. A default or special-case object can turn two ordinary branches into one polymorphic path.

### Minimize `null`

Do not return `null` when absence has a sensible neutral value:

- Return `List.of()` for no items.
- Return `BigDecimal.ZERO` for no monetary adjustment when zero is semantically correct.
- Return an empty domain collection or explicit no-op strategy where that faithfully models the answer.
- Use `Optional<T>` for an expected missing singular result at a repository or query boundary when that matches project conventions.

Do not use `Optional` mechanically for fields, parameters, collections, or every internal helper.

`null` is not universally forbidden. It may be an intentional answer in a tightly controlled API where the caller's logic explicitly checks for it, but it must not force defensive pyramids across the codebase. When absence means something has actually gone wrong, throw a meaningful exception instead of failing silently with `null`.

## Spring Boot Boundaries

### Keep controllers thin and explicit

A controller should handle HTTP concerns: request mapping, transport validation, authentication context, status codes, and request/response DTO conversion. It should invoke one application use case and not contain domain decisions, persistence queries, SDK calls, or transaction scripts.

Prefer constructor injection. Avoid field injection because hidden mutable dependencies make construction and unit testing harder.

### Put transactions around use cases

Place `@Transactional` at the application-service operation that defines the atomic business use case, unless the project has another established boundary. Do not spread transaction annotations across private helpers or controllers. Keep network calls outside long database transactions when correctness permits it.

### Protect the domain from transport and persistence shapes

Do not let `HttpServletRequest`, `ResponseEntity`, Jackson nodes, JPA proxies, or vendor DTOs leak through the application. Use request/response DTOs at HTTP boundaries, domain values for business decisions, and repository interfaces or adapters for persistence.

Java records are good immutable carriers when the values form a transparent data concept. They are not automatically domain objects; add validation or value types where invariants matter.

### Wrap third-party libraries at the boundary

Every third-party SDK is a change boundary. Do not import a payment, storage, email, AI, or messaging SDK throughout the codebase. Define the small API the application actually needs—such as `charge`, `refund`, `put`, or `sendOrderConfirmation`—and contain vendor calls, data conversion, retries, and exception translation in one adapter.

Do not wrap stable Java or Spring APIs just to hide them. Wrap dependencies whose vocabulary, failures, release cycle, or testability should not shape the domain.

### Write the interface the application needs

When a provider or collaborating team is not ready, define the consumer-owned interface from the use case's needs. Build application logic against it and use a fake in tests. When the real provider arrives, write one adapter that translates units, identifiers, tokens, payloads, and errors.

The completed application logic should not change merely because the provider's API has a different shape.

Do not create interfaces for every Spring service. A consumer-owned port is justified by an external boundary, multiple meaningful implementations, an unavailable collaborator, or a real substitution need—not by habit.

### Keep public and private boundaries intentional

Every class is a cooperative context of methods and shared data. Its public API should describe high-level, stable capabilities; its private methods should contain lower-level details expected to change.

Do not expose a generic helper such as `send(...)` merely because another class wants to reuse it. Add the missing domain operation—such as `sendOrderConfirmation(...)`—to the owning context and keep delivery mechanics private. When the delivery format changes, the change then remains contained.

Public means “a supported promise,” not “convenient to call.” Use the narrowest Java visibility that works.

## Tests That Keep Code Changeable

### Practice red, green, refactor

For behavior work:

1. Write only enough test to fail for the intended reason.
2. Write only enough production code to pass.
3. Refactor names, duplication, structure, and design while green.
4. Add the next behavior and repeat.

The first implementation may be deliberately naive if the next test will force the missing rule. Tests stay one step ahead of production code.

### Keep test code clean

Tests are the first place developers work when changing behavior. Tangled fixtures and unreadable setup make small changes expensive, encourage skipped tests, and eventually destroy trust. Apply naming, duplication, abstraction, and formatting discipline to tests too.

Tests do not prove an implementation is universally correct. Their crucial design value is giving the team enough confidence to optimize, refactor, and replace working code without fear.

### Structure tests as world, action, claim

Use Given/When/Then or Arrange/Act/Assert:

- Build the world as declarative facts about relevant state, not a transcript of construction mechanics.
- Run the core action once.
- Make the final claim directly, without unrelated unpacking or logging.

```java
@Test
void rejectsCardThatExpiredBeforeToday() {
    Clock clock = fixedAt("2026-01-15T00:00:00Z");
    Card card = cardExpiring(YearMonth.of(2020, 12));

    CardValidation result = validator(clock).validate(card);

    assertThat(result).isEqualTo(CardValidation.EXPIRED);
}
```

Move mechanical object construction into focused test builders or fixture helpers when that makes the facts clearer. Do not create a fixture framework for one simple object.

### Test one concept at a time

Name a test for the behavior it guards, not merely the production method it calls. Split unrelated promises into separate tests.

Prefer:

- `newMemberStartsInactive`
- `rawPasswordIsNeverExposed`
- `duplicateEmailIsRejected`

over one `createMemberWorks` test with assertions for all three. A failure should name the broken promise while proving the other promises still pass.

### Keep unit tests FIRST

Unit tests should be:

- **Fast:** quick enough to run continuously.
- **Independent:** no test prepares state for another; no shared mutable ordering.
- **Repeatable:** deterministic on a laptop and in CI without dependence on real networks, clocks, randomness, locale, or time zone.
- **Self-validating:** an unambiguous green or red result with assertions, not manual inspection of logs.
- **Timely:** written immediately before the production code that makes them pass.

In Spring Boot, keep pure domain and service tests free of the application context when possible. Use Mockito or small fakes only at genuine boundaries. Use `@WebMvcTest`, `@DataJpaTest`, full `@SpringBootTest`, and Testcontainers when the behavior actually depends on Spring wiring, serialization, database mappings, transactions, or real infrastructure semantics. A slow integration layer complements fast unit tests; it does not replace them.

## Completion Checklist

Before handing off Java/Spring Boot code, verify:

- Names reveal intent, tell the truth, are distinct, pronounceable, searchable, and free of type encodings or private shorthand.
- Method names describe purpose one level above implementation.
- Methods are small, do one thing, and stay at one abstraction level.
- Type switches are not duplicated; polymorphism is used only where type evolution warrants it.
- Argument counts and ordering are easy to understand; related triads have become a named concept.
- No control flags, output arguments, hidden side effects, or mixed command/query contracts remain.
- Shared knowledge has one authoritative representation.
- Comments carry information the code cannot express, stay local and accurate, and contain no dead code or fake history.
- Formatting, spacing, declarations, and member order make the code scan naturally.
- Encapsulation exposes behavior rather than storage; JPA/domain roles are deliberate; object/data trade-offs match the likely change axis.
- Demeter chains have been replaced by meaningful owner operations where objects—not DTOs—are involved.
- Happy-path logic is separate from error handling; application-owned exceptions replace error codes and vendor exception leakage.
- Expected alternatives do not use exceptions; absence uses empty values, `Optional`, a special case, `null`, or an exception according to its real semantics.
- Controllers, transactions, domain logic, persistence, and vendor adapters have honest boundaries.
- Public APIs expose stable domain operations; low-level changing details remain private.
- Tests are clean, behavior-named, one-concept, world/action/claim, fast, independent, repeatable, self-validating, and timely.
- Relevant tests, formatting, static analysis, and build checks pass.

## Transcript Coverage Map

This skill intentionally retains all 51 supplied lessons:

1. Intent-revealing names → **Reveal intent**.
2. Avoid disinformation → **Do not misinform**.
3. Meaningful distinctions → **Make meaningful distinctions**.
4. Pronounceable names → **Use pronounceable names**.
5. Searchable names and scope-sized naming → **Use searchable names**.
6. Avoid Hungarian/type encodings → **Avoid type encodings and mental mapping**.
7. Avoid mental mapping and cryptic abbreviations → **Avoid type encodings and mental mapping**.
8. Small methods and one-line delegation from blocks → **Keep methods small, then look for smaller**.
9. One responsibility, section test, extraction test → **Do one thing**.
10. One abstraction level and top-down reading → **Stay at one level of abstraction**.
11. Centralize switches and use polymorphism → **Keep type dispatch in one place**.
12. Zero/one/two/three-argument guidance and parameter objects → **Keep argument lists light**.
13. Boolean flags, output arguments, and one-argument shapes → **Do not pass control flags** and **Avoid output arguments**.
14. Natural dyads, dangerous triads, rigid ordering, and noun-bearing names → **Make dyads and triads obvious**.
15. Hidden side effects make methods lie → **Keep side effects honest**.
16. Command/query separation → **Separate commands from queries**.
17. Exceptions over error codes and separation of processing from handling → **Prefer exceptions to error codes**.
18. DRY as one authoritative representation of knowledge → **Remove duplicated knowledge**.
19. Clean code is rewritten under tests → **Rewrite under protection**.
20. Comments drift and lie → **Treat every comment as a maintenance liability**.
21. Business explanations, translations, complex formulas, and accuracy → **Write comments only for information code cannot express**.
22. Warnings, amplification, and reasoned TODOs → **Write comments only for information code cannot express**.
23. Public documentation, copyright, and standard licenses → **Write comments only for information code cannot express**.
24. Redundant, noise, and mandated comments create clutter, maintenance, and blindness → **Delete comments that add no knowledge**.
25. Misleading, obscure, and non-local comments → **Delete comments that add no knowledge**.
26. Commented code, attribution, and journal history belong in version control → **Delete commented-out code and fake history**.
27. Closing-brace and position-marker comments expose structural problems, with rare framework exceptions → **Do not use comments to apologize for structure**.
28. Mumbling, irrelevant history, and HTML-heavy comments → **Delete comments that add no knowledge**.
29. Replace comments with names, extracted methods, and named constants → **Treat every comment as a maintenance liability**.
30. Newspaper ordering, caller-before-callee flow, and conceptual affinity → **Make files read like newspapers**.
31. Vertical openness, density, and indentation → **Use vertical spacing to show concepts**.
32. Local declarations near use, fields in a designated place, and team formatting consistency → **Declare things close to use**.
33. Getters/setters do not equal abstraction; expose behavior and atomic access policies → **Do not turn private fields into public structure with boilerplate accessors**.
34. Procedural data versus objects and their inverse change costs → **Choose objects or data structures deliberately**.
35. Law of Demeter, objects versus data, and tell the owner what outcome is needed → **Follow the Law of Demeter for objects**.
36. Object/data hybrids and business logic inside active-record-like data → **Avoid hybrids**.
37. Separate the algorithm from error handling → **Prefer exceptions to error codes**.
38. Design try/catch/finally and its test as a failure contract first → **Define the failure contract before risky logic**.
39. Translate library exception shapes into application-owned exception design → **Own exception types at external boundaries**.
40. Do not use exceptions for planned control flow; return a default rule/special case → **Do not use exceptions as ordinary branches**.
41. Avoid defensive null pyramids; use neutral values, intentional absence, or exceptions → **Minimize `null`**.
42. Wrap third-party SDKs behind application vocabulary → **Wrap third-party libraries at the boundary**.
43. Write the consumer-owned interface, test with a fake, and adapt the eventual provider → **Write the interface the application needs**.
44. Red-green-refactor with the smallest failing test and passing code → **Practice red, green, refactor**.
45. Tests must also be clean and readable → **Keep test code clean**.
46. Tests make working code safe to change → **Keep test code clean**.
47. Build the world, run one action, make one clear claim → **Structure tests as world, action, claim**.
48. One concept per behavior-named test → **Test one concept at a time**.
49. Fast, independent, repeatable, self-validating, and timely tests → **Keep unit tests FIRST**.
50. Stable high-level public interface and changing low-level private internals → **Keep public and private boundaries intentional**.
51. Name methods for purpose above mechanism → **Name methods one abstraction above their implementation**.
